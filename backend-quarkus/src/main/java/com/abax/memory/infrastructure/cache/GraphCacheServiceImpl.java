package com.abax.memory.infrastructure.cache;

import com.abax.memory.domain.model.GraphExpansionResult;
import com.abax.memory.domain.model.GraphMutatedEvent;
import com.abax.memory.domain.service.GraphCacheService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Caffeine-based implementation of {@link GraphCacheService} — v2.1.0.
 *
 * <p>Uses a single Caffeine cache instance with configurable TTL,
 * max size, and LRU eviction. Invalidates entries reactively via
 * {@code @ObservesAsync} on {@link GraphMutatedEvent}.</p>
 *
 * <p>Cache key is a deterministic hash of: sorted entryPoint IDs + depth + sorted kinds.
 * This ensures that semantically identical graph expansion requests hit the cache
 * regardless of entry point ordering.</p>
 *
 * <p>References: ADR-005, FT-V21-002.1</p>
 */
@ApplicationScoped
public class GraphCacheServiceImpl implements GraphCacheService {

    private static final Logger LOG = Logger.getLogger(GraphCacheServiceImpl.class);

    private final Cache<String, GraphExpansionResult> cache;

    // Configurable via constructor (from InfrastructureConfig)
    public GraphCacheServiceImpl(long ttlSeconds, long maxSize) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .maximumSize(maxSize)
                .recordStats()
                .removalListener((key, value, cause) ->
                        LOG.debugv("Graph cache eviction: key={0}, cause={1}", key, cause))
                .build();
        LOG.infov("GraphCacheService initialized: ttl={0}s, maxSize={1}", ttlSeconds, maxSize);
    }

    @Override
    public String buildKey(Set<UUID> entryPointIds, int depth, Set<String> includeKinds) {
        // Sort entry point IDs for deterministic order
        List<String> sortedIds = entryPointIds.stream()
                .map(UUID::toString)
                .sorted()
                .toList();

        // Sort kinds if present
        List<String> sortedKinds = includeKinds != null
                ? includeKinds.stream().sorted().toList()
                : List.of();

        // Build canonical string: sortedIds|depth|sortedKinds
        String canonical = sortedIds + "|" + depth + "|" + sortedKinds;

        // Hash with SHA-256 for compact, fixed-length key
        return sha256(canonical);
    }

    @Override
    public GraphExpansionResult get(String key) {
        GraphExpansionResult result = cache.getIfPresent(key);
        if (result != null) {
            LOG.debugv("Graph cache HIT: key={0}", key);
        } else {
            LOG.debugv("Graph cache MISS: key={0}", key);
        }
        return result;
    }

    @Override
    public void put(String key, GraphExpansionResult result) {
        cache.put(key, result);
        LOG.debugv("Graph cache PUT: key={0}", key);
    }

    @Override
    public void invalidateByMemoryId(UUID memoryId) {
        // Scan all entries and invalidate those whose entryPoint IDs
        // (embedded in the key hash) contain the affected memoryId.
        // Since we can't reverse the hash, we use a simpler approach:
        // invalidate ALL entries when any relation is mutated.
        // This is conservative but correct for the COULD priority.
        long sizeBefore = cache.estimatedSize();
        cache.invalidateAll();
        LOG.infov("Graph cache INVALIDATE ALL due to mutation on memoryId={0}. "
                + "Entries evicted: {1}", memoryId, sizeBefore);
    }

    @Override
    public Map<String, Object> getMetrics() {
        CacheStats stats = cache.stats();
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("hitRatio", stats.hitRate());
        metrics.put("hitCount", stats.hitCount());
        metrics.put("missCount", stats.missCount());
        metrics.put("size", cache.estimatedSize());
        metrics.put("evictions", stats.evictionCount());
        return metrics;
    }

    /**
     * Observes graph mutation events asynchronously to avoid blocking
     * the relation creation/deletion transaction.
     */
    void onGraphMutated(@ObservesAsync GraphMutatedEvent event) {
        LOG.debugv("Graph cache invalidation triggered by {0}: source={1}, target={2}",
                event.getMutationType(), event.getSourceId(), event.getTargetId());
        invalidateByMemoryId(event.getSourceId());
        invalidateByMemoryId(event.getTargetId());
    }

    // ── Private helpers ──────────────────────────────────────────────

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to exist in every JVM
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}

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
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine-based implementation of {@link GraphCacheService} — v2.1.0.
 *
 * <p>Configuration via {@code @ConfigProperty} on InfrastructureConfig producer.
 * Observer invalidates cache entries on relation mutations.</p>
 *
 * <p>References: ADR-005, FT-V21-002.1</p>
 */
@ApplicationScoped
public class GraphCacheServiceImpl implements GraphCacheService {

    private static final Logger LOG = Logger.getLogger(GraphCacheServiceImpl.class);

    private final Cache<String, GraphExpansionResult> cache;

    // Default no-arg for CDI — config set via init
    private long ttlSeconds = 300;
    private long maxSize = 1000;

    /** CDI-compatible constructor — config set via setters or producer. */
    public GraphCacheServiceImpl() {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .maximumSize(maxSize)
                .recordStats()
                .removalListener((key, value, cause) ->
                        LOG.debugv("Graph cache eviction: key={0}, cause={1}", key, cause))
                .build();
    }

    /** Test constructor — bypasses CDI config. */
    GraphCacheServiceImpl(long ttlSeconds, long maxSize) {
        this.ttlSeconds = ttlSeconds;
        this.maxSize = maxSize;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .maximumSize(maxSize)
                .recordStats()
                .removalListener((key, value, cause) ->
                        LOG.debugv("Graph cache eviction: key={0}, cause={1}", key, cause))
                .build();
        LOG.infov("GraphCacheService initialized (test): ttl={0}s, maxSize={1}", ttlSeconds, maxSize);
    }

    @Override
    public String buildKey(Set<UUID> entryPointIds, int depth, Set<String> includeKinds) {
        List<String> sortedIds = entryPointIds.stream()
                .map(UUID::toString).sorted().toList();
        List<String> sortedKinds = includeKinds != null
                ? includeKinds.stream().sorted().toList()
                : List.of();
        String canonical = sortedIds + "|" + depth + "|" + sortedKinds;
        return sha256(canonical);
    }

    @Override
    public GraphExpansionResult get(String key) {
        GraphExpansionResult result = cache.getIfPresent(key);
        LOG.debugv("Graph cache {0}: key={1}", result != null ? "HIT" : "MISS", key);
        return result;
    }

    @Override
    public void put(String key, GraphExpansionResult result) {
        cache.put(key, result);
        LOG.debugv("Graph cache PUT: key={0}", key);
    }

    @Override
    public void invalidateByMemoryId(UUID memoryId) {
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

    void onGraphMutated(@ObservesAsync GraphMutatedEvent event) {
        LOG.debugv("Graph cache invalidation triggered by {0}: source={1}, target={2}",
                event.getMutationType(), event.getSourceId(), event.getTargetId());
        invalidateByMemoryId(event.getSourceId());
        invalidateByMemoryId(event.getTargetId());
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}

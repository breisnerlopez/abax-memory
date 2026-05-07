package com.abax.memory.domain.service;

import com.abax.memory.domain.model.GraphExpansionResult;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Service contract for caching graph expansion results — v2.1.0.
 *
 * <p>Reduces BFS latency for repeated queries sharing entry points,
 * depth, and kind filters. Uses Caffeine in-memory cache with
 * configurable TTL and LRU eviction. Invalidated reactively via
 * CDI events on {@code RelationEntity} mutations.</p>
 *
 * <p>References: ADR-005, FT-V21-002.1</p>
 */
public interface GraphCacheService {

    /**
     * Builds a deterministic cache key from graph expansion parameters.
     *
     * @param entryPointIds set of seed node UUIDs (sorted internally)
     * @param depth         BFS depth
     * @param includeKinds  set of relation kinds to include (nullable)
     * @return cache key string
     */
    String buildKey(Set<UUID> entryPointIds, int depth, Set<String> includeKinds);

    /**
     * Retrieves a cached graph expansion result.
     *
     * @param key cache key from {@link #buildKey}
     * @return cached result, or {@code null} if miss
     */
    GraphExpansionResult get(String key);

    /**
     * Stores a graph expansion result in the cache.
     *
     * @param key    cache key
     * @param result expansion result to cache
     */
    void put(String key, GraphExpansionResult result);

    /**
     * Invalidates all cache entries whose entry point IDs include
     * either {@code sourceId} or {@code targetId} of a mutated relation.
     *
     * @param memoryId UUID of the affected memory fragment
     */
    void invalidateByMemoryId(UUID memoryId);

    /**
     * Returns cache metrics for observability.
     *
     * @return map with keys: hitRatio, size, evictions
     */
    Map<String, Object> getMetrics();
}

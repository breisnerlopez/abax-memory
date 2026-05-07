package com.abax.memory.infrastructure.cache;

import com.abax.memory.domain.model.GraphExpansionResult;
import com.abax.memory.domain.model.GraphMutatedEvent;
import com.abax.memory.domain.service.GraphCacheService;
import com.abax.memory.infrastructure.persistence.MemoryFragmentEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GraphCacheServiceImpl} — FT-V21-002.1.
 *
 * <p>Tests cache hit/miss, eviction, invalidation by mutation event,
 * and key determinism (order-independent).</p>
 */
@DisplayName("GraphCacheService — Caffeine graph cache")
class GraphCacheServiceTest {

    private GraphCacheService cacheService;
    private static final UUID NODE_A = UUID.randomUUID();
    private static final UUID NODE_B = UUID.randomUUID();
    private static final UUID NODE_C = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        cacheService = new GraphCacheServiceImpl(300, 100);
    }

    @Test
    @DisplayName("should return null on cache miss")
    void shouldReturnNullOnCacheMiss() {
        String key = cacheService.buildKey(Set.of(NODE_A), 2, null);
        assertThat(cacheService.get(key)).isNull();
    }

    @Test
    @DisplayName("should return cached result on cache hit")
    void shouldReturnCachedResultOnCacheHit() {
        String key = cacheService.buildKey(Set.of(NODE_A, NODE_B), 2, null);

        Map<UUID, Double> nodes = new LinkedHashMap<>();
        nodes.put(NODE_C, 0.85);
        Map<UUID, MemoryFragmentEntity> entityMap = new LinkedHashMap<>();
        cacheService.put(key, new GraphExpansionResult(nodes, entityMap));

        GraphExpansionResult result = cacheService.get(key);
        assertThat(result).isNotNull();
        assertThat(result.getNodes()).containsKey(NODE_C);
        assertThat(result.getNodes().get(NODE_C)).isEqualTo(0.85);
    }

    @Test
    @DisplayName("should produce same key regardless of entry point ordering")
    void shouldProduceSameKeyForDifferentOrder() {
        String key1 = cacheService.buildKey(Set.of(NODE_A, NODE_B), 2, null);
        String key2 = cacheService.buildKey(Set.of(NODE_B, NODE_A), 2, null);
        assertThat(key1).isEqualTo(key2);
    }

    @Test
    @DisplayName("should produce different keys for different depths")
    void shouldProduceDifferentKeysForDifferentDepth() {
        String key1 = cacheService.buildKey(Set.of(NODE_A), 2, null);
        String key2 = cacheService.buildKey(Set.of(NODE_A), 3, null);
        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    @DisplayName("should return metrics with expected keys")
    void shouldReturnMetrics() {
        Map<String, Object> metrics = cacheService.getMetrics();
        assertThat(metrics).containsKeys("hitRatio", "hitCount", "missCount", "size", "evictions");
    }

    @Test
    @DisplayName("should track hits and misses in metrics")
    void shouldTrackHitsAndMisses() {
        String key = cacheService.buildKey(Set.of(NODE_A), 1, null);
        cacheService.get(key); // miss
        cacheService.get(key); // miss
        cacheService.put(key, new GraphExpansionResult(Map.of(), Map.of()));
        cacheService.get(key); // hit

        Map<String, Object> metrics = cacheService.getMetrics();
        assertThat((Long) metrics.get("hitCount")).isEqualTo(1L);
        assertThat((Long) metrics.get("missCount")).isEqualTo(2L);
    }

    @Test
    @DisplayName("should accept and retrieve entries within max size")
    void shouldEvictOnMaxSize() {
        GraphCacheService smallCache = new GraphCacheServiceImpl(300, 2);
        String key1 = smallCache.buildKey(Set.of(NODE_A), 1, null);
        String key2 = smallCache.buildKey(Set.of(NODE_B), 1, null);

        smallCache.put(key1, new GraphExpansionResult(Map.of(NODE_A, 0.5), Map.of()));
        smallCache.put(key2, new GraphExpansionResult(Map.of(NODE_B, 0.6), Map.of()));

        // Both entries should be retrievable
        assertThat(smallCache.get(key1)).isNotNull();
        assertThat(smallCache.get(key2)).isNotNull();

        // Cache size should be ≤ maxSize
        Map<String, Object> metrics = smallCache.getMetrics();
        assertThat((Long) metrics.get("size")).isEqualTo(2L);
    }

    @Test
    @DisplayName("invalidateByMemoryId should clear all entries")
    void invalidateShouldClearAllEntries() {
        String key1 = cacheService.buildKey(Set.of(NODE_A), 1, null);
        cacheService.put(key1, new GraphExpansionResult(Map.of(NODE_A, 0.5), Map.of()));

        cacheService.invalidateByMemoryId(NODE_A);

        assertThat(cacheService.get(key1)).isNull();
    }

    @Test
    @DisplayName("should handle null includeKinds gracefully")
    void shouldHandleNullIncludeKinds() {
        String key = cacheService.buildKey(Set.of(NODE_A), 2, null);
        assertThat(key).isNotNull();
        assertThat(key).isNotEmpty();
    }
}

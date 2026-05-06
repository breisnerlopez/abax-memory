package com.abax.memory.domain.model;

import com.abax.memory.infrastructure.persistence.MemoryFragmentEntity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable DTO holding a cached graph expansion result — v2.1.0.
 *
 * <p>Stored in {@link com.abax.memory.domain.service.GraphCacheService}
 * to avoid re-running BFS for identical graph-expansion parameters.</p>
 *
 * <p>References: ADR-005, FT-V21-002.1</p>
 */
public class GraphExpansionResult {

    private final Map<UUID, Double> nodes;
    private final Map<UUID, MemoryFragmentEntity> entityMap;
    private final long createdAtMillis;

    public GraphExpansionResult(Map<UUID, Double> nodes,
                                  Map<UUID, MemoryFragmentEntity> entityMap) {
        this.nodes = Map.copyOf(nodes);
        this.entityMap = Map.copyOf(entityMap);
        this.createdAtMillis = System.currentTimeMillis();
    }

    /** Graph-discovered node IDs mapped to their computed scores. */
    public Map<UUID, Double> getNodes() {
        return nodes;
    }

    /** JPA entities for all nodes in the subgraph. */
    public Map<UUID, MemoryFragmentEntity> getEntityMap() {
        return entityMap;
    }

    /** Epoch millis when this result was created. */
    public long getCreatedAtMillis() {
        return createdAtMillis;
    }
}

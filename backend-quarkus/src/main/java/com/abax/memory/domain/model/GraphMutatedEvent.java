package com.abax.memory.domain.model;

import java.util.UUID;

/**
 * CDI event fired when a relation is created or deleted — v2.1.0.
 *
 * <p>Observed by {@link com.abax.memory.domain.service.GraphCacheService}
 * to invalidate affected graph cache entries.</p>
 *
 * <p>References: ADR-005, FT-V21-002.1</p>
 */
public class GraphMutatedEvent {

    private final UUID sourceId;
    private final UUID targetId;
    private final String mutationType; // "CREATE" or "DELETE"

    public GraphMutatedEvent(UUID sourceId, UUID targetId, String mutationType) {
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.mutationType = mutationType;
    }

    public UUID getSourceId() { return sourceId; }
    public UUID getTargetId() { return targetId; }
    public String getMutationType() { return mutationType; }
}

package com.abax.memory.domain.model;

import com.abax.memory.domain.enums.RelationType;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain model for a typed, directed relationship between
 * two MemoryFragments — v2.0.0.
 *
 * <p>References: EP-001, FT-001.03, §2.5 of functional spec</p>
 */
public class Relation {

    private UUID id;
    private UUID sourceId;
    private UUID targetId;
    private RelationType type;
    private String tenantId;
    private Instant createdAt;

    public Relation() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSourceId() { return sourceId; }
    public void setSourceId(UUID sourceId) { this.sourceId = sourceId; }

    public UUID getTargetId() { return targetId; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }

    public RelationType getType() { return type; }
    public void setType(RelationType type) { this.type = type; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

package com.abax.memory.domain.model;

import com.abax.memory.domain.enums.RelationType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Domain model for a typed, directed relationship between
 * two MemoryFragments — v2.0.0.
 *
 * <p>New in v2.1.0: added {@code weight}, {@code metadata}, and
 * {@code updatedAt} fields for CP-V21-024 (Relation modification API).</p>
 *
 * <p>References: EP-001, FT-001.03, §2.5 of functional spec, CP-V21-024</p>
 */
public class Relation {

    private UUID id;
    private UUID sourceId;
    private UUID targetId;
    private RelationType type;
    private Double weight;
    private Map<String, Object> metadata;
    private String tenantId;
    private Instant createdAt;
    private Instant updatedAt;

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

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

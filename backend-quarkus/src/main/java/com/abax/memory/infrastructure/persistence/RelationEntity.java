package com.abax.memory.infrastructure.persistence;

import com.abax.memory.domain.enums.RelationType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapped to {@code relations} table — typed, directed
 * relationships between memory fragments (Flyway V3).
 *
 * <p>Uses Panache active-record pattern ({@link PanacheEntityBase})
 * for concise repository operations.</p>
 *
 * <p>References: EP-005, Flyway V3, DDL §4.1.3 of architecture document.</p>
 */
@Entity
@Table(name = "relations", indexes = {
        @Index(name = "idx_relations_source", columnList = "source_id"),
        @Index(name = "idx_relations_target", columnList = "target_id"),
        @Index(name = "idx_relations_tenant", columnList = "tenant_id"),
        @Index(name = "idx_relations_type", columnList = "source_id, relation_type"),
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_relations", columnNames = {"source_id", "target_id", "relation_type"})
})
public class RelationEntity extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private UUID id;

    @Column(name = "source_id", nullable = false, length = 36)
    private UUID sourceId;

    @Column(name = "target_id", nullable = false, length = 36)
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 30)
    private RelationType relationType;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ─── Panache-required default constructor ────────────────────────

    public RelationEntity() {
    }

    // ─── Lifecycle callbacks ─────────────────────────────────────────

    @PrePersist
    void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        this.createdAt = (this.createdAt != null) ? this.createdAt : Instant.now();
    }

    // ─── Getters / Setters ───────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSourceId() { return sourceId; }
    public void setSourceId(UUID sourceId) { this.sourceId = sourceId; }

    public UUID getTargetId() { return targetId; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }

    public RelationType getRelationType() { return relationType; }
    public void setRelationType(RelationType relationType) { this.relationType = relationType; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

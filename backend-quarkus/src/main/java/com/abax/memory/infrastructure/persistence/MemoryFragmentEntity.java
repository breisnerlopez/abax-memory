package com.abax.memory.infrastructure.persistence;

import com.abax.memory.domain.enums.LifecycleState;
import com.abax.memory.domain.enums.MemoryKind;
import com.abax.memory.domain.enums.SensitivityLevel;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapped to {@code memory_fragments} — the central table
 * of Abax-Memory v2.0.0.
 *
 * <p>Uses Panache active-record pattern ({@link PanacheEntityBase})
 * for concise repository operations.</p>
 *
 * <p>Soft-delete is implemented via {@link #deletedAt}. Queries
 * must filter by {@code deletedAt IS NULL} to exclude soft-deleted
 * records.</p>
 *
 * <p>References: EP-001, DDL §4.1.1 of architecture document.</p>
 */
@Entity
@Table(name = "memory_fragments", indexes = {
        @Index(name = "idx_mem_frag_tenant", columnList = "tenant_id"),
        @Index(name = "idx_mem_frag_tenant_state", columnList = "tenant_id, lifecycle_state"),
        @Index(name = "idx_mem_frag_tenant_kind", columnList = "tenant_id, kind"),
        @Index(name = "idx_mem_frag_created_at", columnList = "tenant_id, created_at DESC"),
})
public class MemoryFragmentEntity extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "scope_id", length = 255)
    private String scopeId;

    @Column(name = "namespace", length = 512)
    private String namespace;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 30)
    private MemoryKind kind;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_state", nullable = false, length = 20)
    private LifecycleState lifecycleState;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensitivity_level", nullable = false, length = 20)
    private SensitivityLevel sensitivityLevel;

    @Column(name = "source_type", length = 50)
    private String sourceType;

    @Column(name = "source_ref", length = 500)
    private String sourceRef;

    @Column(name = "confidence", nullable = false)
    private Double confidence;

    @Column(name = "embedding_id", length = 255)
    private String embeddingId;

    @Column(name = "reviewer_id", length = 255)
    private String reviewerId;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // ─── Panache-required default constructor ────────────────────────

    public MemoryFragmentEntity() {
    }

    // ─── Lifecycle callbacks ─────────────────────────────────────────

    @PrePersist
    void onCreate() {
        var now = Instant.now();
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        this.createdAt = (this.createdAt != null) ? this.createdAt : now;
        this.updatedAt = now;
        if (this.lifecycleState == null) {
            this.lifecycleState = LifecycleState.DRAFT;
        }
        if (this.sensitivityLevel == null) {
            this.sensitivityLevel = SensitivityLevel.INTERNAL;
        }
        if (this.confidence == null) {
            this.confidence = 0.5;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // ─── Soft-delete helpers ─────────────────────────────────────────

    /** Marks this fragment as soft-deleted. Does NOT persist — caller must do so. */
    public void softDelete() {
        this.deletedAt = Instant.now();
        this.lifecycleState = LifecycleState.DELETED;
    }

    /** Returns {@code true} when this record has been soft-deleted. */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    // ─── Getters / Setters ───────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getScopeId() { return scopeId; }
    public void setScopeId(String scopeId) { this.scopeId = scopeId; }

    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }

    public MemoryKind getKind() { return kind; }
    public void setKind(MemoryKind kind) { this.kind = kind; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public LifecycleState getLifecycleState() { return lifecycleState; }
    public void setLifecycleState(LifecycleState lifecycleState) { this.lifecycleState = lifecycleState; }

    public SensitivityLevel getSensitivityLevel() { return sensitivityLevel; }
    public void setSensitivityLevel(SensitivityLevel sensitivityLevel) { this.sensitivityLevel = sensitivityLevel; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getSourceRef() { return sourceRef; }
    public void setSourceRef(String sourceRef) { this.sourceRef = sourceRef; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public String getEmbeddingId() { return embeddingId; }
    public void setEmbeddingId(String embeddingId) { this.embeddingId = embeddingId; }

    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }

    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

    @Override
    public String toString() {
        return "MemoryFragmentEntity{" +
               "id=" + id +
               ", tenantId='" + tenantId + '\'' +
               ", kind=" + kind +
               ", lifecycleState=" + lifecycleState +
               ", title='" + title + '\'' +
               '}';
    }
}

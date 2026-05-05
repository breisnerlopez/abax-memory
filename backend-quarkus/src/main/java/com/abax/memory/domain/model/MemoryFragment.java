package com.abax.memory.domain.model;

import com.abax.memory.domain.enums.LifecycleState;
import com.abax.memory.domain.enums.MemoryKind;
import com.abax.memory.domain.enums.SensitivityLevel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Domain model for a MemoryFragment — the central entity
 * of Abax-Memory v2.0.0.
 *
 * <p>This is a pure domain POJO. Persistence is handled by
 * {@link com.abax.memory.infrastructure.persistence.MemoryFragmentEntity}.</p>
 *
 * <p>References: EP-001, §2.1 of functional spec</p>
 */
public class MemoryFragment {

    private UUID id;
    private String tenantId;
    private String scopeId;
    private MemoryKind kind;
    private String title;
    private String content;
    private String summary;
    private LifecycleState lifecycleState;
    private SensitivityLevel sensitivityLevel;
    private String sourceType;
    private String sourceRef;
    private Double confidence;
    private String embeddingId;
    private String reviewerId;
    private String reviewComment;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    private final List<Relation> relations = new ArrayList<>();

    public MemoryFragment() {
    }

    // ── Getters / Setters ──────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getScopeId() { return scopeId; }
    public void setScopeId(String scopeId) { this.scopeId = scopeId; }

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

    public List<Relation> getRelations() { return relations; }

    // ── Domain helpers ─────────────────────────────────────────

    public boolean isDeleted() {
        return deletedAt != null || lifecycleState == LifecycleState.DELETED;
    }

    public boolean isConsumerVisible() {
        return !isDeleted() && lifecycleState != null && lifecycleState.isConsumerVisible();
    }
}

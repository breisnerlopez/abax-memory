package com.abax.memory.api.dto.v2;

import com.abax.memory.domain.enums.LifecycleState;
import com.abax.memory.domain.enums.MemoryKind;
import com.abax.memory.domain.enums.SensitivityLevel;
import com.abax.memory.infrastructure.persistence.MemoryFragmentEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a single MemoryFragment — includes all entity fields
 * plus calculated fields {@code isDeleted} and {@code isConsumerVisible}.
 *
 * <p>References: HU-004.2.1, API Design §7.2</p>
 */
public record MemoryResponse(
        UUID id,
        String tenantId,
        String scopeId,
        String namespace,
        MemoryKind kind,
        String title,
        String content,
        String summary,
        LifecycleState lifecycleState,
        SensitivityLevel sensitivityLevel,
        String sourceType,
        String sourceRef,
        Double confidence,
        String embeddingId,
        String reviewerId,
        String reviewComment,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        boolean isDeleted,
        boolean isConsumerVisible,
        Double score
) {

    /**
     * Factory method that maps a JPA entity to a MemoryResponse DTO.
     */
    public static MemoryResponse from(MemoryFragmentEntity entity) {
        return new MemoryResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getScopeId(),
                entity.getNamespace(),
                entity.getKind(),
                entity.getTitle(),
                entity.getContent(),
                entity.getSummary(),
                entity.getLifecycleState(),
                entity.getSensitivityLevel(),
                entity.getSourceType(),
                entity.getSourceRef(),
                entity.getConfidence(),
                entity.getEmbeddingId(),
                entity.getReviewerId(),
                entity.getReviewComment(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt(),
                entity.isDeleted(),
                entity.isDeleted()
                        ? false
                        : entity.getLifecycleState() != null && entity.getLifecycleState().isConsumerVisible(),
                null  // score is set by search services, not from entity
        );
    }
}

package com.abax.memory.api.dto.v2;

import com.abax.memory.domain.enums.RelationType;
import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.Map;

/**
 * Request DTO for PATCH /api/v2/relations/{id} — partial update of a relationship.
 *
 * <p>Only non-null fields are applied. At least one field must be non-null.
 * For full replacement, use {@link UpdateRelationRequest} via PUT.</p>
 *
 * <p>New in v2.1.0 — CP-V21-024 (Gap 1: Relation modification API).</p>
 *
 * <p>References: CP-V21-024, HU-001.8.3</p>
 */
public record PatchRelationRequest(
        @JsonAlias("type")
        RelationType relationType,

        Double weight,

        Map<String, Object> metadata
) {
    /**
     * Returns {@code true} if at least one field is non-null.
     */
    public boolean hasAnyField() {
        return relationType != null || weight != null || metadata != null;
    }
}

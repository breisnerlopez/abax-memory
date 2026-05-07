package com.abax.memory.api.dto.v2;

import com.abax.memory.domain.enums.RelationType;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request DTO for PUT /api/v2/relations/{id} — full update of a relationship.
 *
 * <p>All fields are required for a full replacement. For partial updates,
 * use {@link PatchRelationRequest} via PATCH.</p>
 *
 * <p>New in v2.1.0 — CP-V21-024 (Gap 1: Relation modification API).</p>
 *
 * <p>References: CP-V21-024, HU-001.8.3</p>
 */
public record UpdateRelationRequest(
        @NotNull(message = "relationType is required")
        @JsonAlias("type")
        RelationType relationType,

        @NotNull(message = "weight is required")
        Double weight,

        Map<String, Object> metadata
) {
}

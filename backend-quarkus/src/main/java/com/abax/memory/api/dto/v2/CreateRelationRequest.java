package com.abax.memory.api.dto.v2;

import com.abax.memory.domain.enums.RelationType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for POST /api/v2/relations — create a relationship.
 *
 * <p>References: HU-001.8.1</p>
 */
public record CreateRelationRequest(
        @NotNull(message = "sourceId is required")
        UUID sourceId,

        @NotNull(message = "targetId is required")
        UUID targetId,

        @NotNull(message = "relationType is required")
        RelationType relationType
) {
}

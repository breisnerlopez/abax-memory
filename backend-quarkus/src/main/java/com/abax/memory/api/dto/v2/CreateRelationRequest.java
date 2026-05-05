package com.abax.memory.api.dto.v2;

import com.abax.memory.domain.enums.RelationType;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for POST /api/v2/relations — create a relationship.
 *
 * <p>Accepts both {@code relationType} (canonical) and {@code type}
 * (alias, for backward compatibility with API consumers) as the
 * JSON field name for the relationship type.</p>
 *
 * <p>References: HU-001.8.1, BUG-004</p>
 */
public record CreateRelationRequest(
        @NotNull(message = "sourceId is required")
        UUID sourceId,

        @NotNull(message = "targetId is required")
        UUID targetId,

        @NotNull(message = "relationType is required")
        @JsonAlias("type")
        RelationType relationType
) {
}

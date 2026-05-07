package com.abax.memory.api.dto.v2;

import com.abax.memory.domain.model.DomainProfile;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for domain profile API endpoints.
 *
 * <p>New in v2.1.0 — CP-V21-041+ (Gap 2: Domain profile management API).</p>
 *
 * <p>References: CP-V21-041+</p>
 */
public record DomainProfileResponse(
        UUID id,
        String name,
        String version,
        String description,
        Map<String, Object> config,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Converts a {@link DomainProfile} domain model to a response DTO.
     */
    public static DomainProfileResponse from(DomainProfile profile) {
        return new DomainProfileResponse(
                profile.getId(),
                profile.getName(),
                profile.getVersion(),
                profile.getDescription(),
                profile.getConfig(),
                profile.isActive(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}

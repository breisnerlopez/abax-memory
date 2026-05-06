package com.abax.memory.api.dto.v2;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request DTO for PUT /api/v2/domains/{name} — create or update a domain profile.
 *
 * <p>The {@code config} field contains the JSON configuration as a Map.
 * It will be serialized to JSONB in the database.</p>
 *
 * <p>New in v2.1.0 — CP-V21-041+ (Gap 2: Domain profile management API).</p>
 *
 * <p>References: CP-V21-041+, HU-002.1</p>
 */
public record DomainProfileRequest(
        @NotNull(message = "config is required")
        Map<String, Object> config,

        String description,

        String version,

        boolean active
) {
}

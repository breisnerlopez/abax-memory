package com.abax.memory.api.dto.v2;

import com.abax.memory.domain.enums.MemoryKind;
import com.abax.memory.domain.enums.SensitivityLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Request DTO for POST /api/v2/memories — create a new MemoryFragment.
 *
 * <p>title and content are required. All other fields are optional
 * and will fall back to sensible defaults if not supplied.</p>
 *
 * <p>References: HU-004.1.1, API Design §7.2</p>
 */
public record CreateMemoryRequest(

        @NotBlank(message = "title is required")
        @Size(max = 500, message = "title must not exceed 500 characters")
        String title,

        @NotBlank(message = "content is required")
        String content,

        MemoryKind kind,

        String scopeId,

        SensitivityLevel sensitivityLevel,

        @Size(max = 50, message = "source_type must not exceed 50 characters")
        String sourceType,

        @Size(max = 500, message = "source_ref must not exceed 500 characters")
        String sourceRef,

        Double confidence,

        Map<String, Object> metadata,

        String namespace
) {
}

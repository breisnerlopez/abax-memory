package com.abax.memory.api.dto.v2;

import com.abax.memory.domain.enums.MemoryKind;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for POST /api/v2/memories/extract — entity extraction.
 *
 * <p>References: HU-005.8.1</p>
 */
public record ExtractRequest(

        @NotBlank(message = "content is required")
        String content,

        MemoryKind kind
) {}

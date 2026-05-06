package com.abax.memory.api.dto.v2;

import com.abax.memory.domain.enums.MemoryKind;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for POST /api/v2/memories/extract — entity extraction.
 *
 * <p>New in v2.1.0: {@code domain} field to guide domain-specific extraction.
 * {@code kind} is retained for backward compatibility.</p>
 *
 * <p>{@code content} also accepts {@code "text"} as a JSON alias for
 * backward compatibility with API consumers who use the legacy field name.</p>
 *
 * <p>References: HU-005.8.1, FT-V21-001.4, DEF-V21-007</p>
 */
public record ExtractRequest(

        @NotBlank(message = "content is required")
        @Size(max = 5000, message = "content exceeds maximum length of 5000 characters")
        @JsonAlias("text")
        String content,

        MemoryKind kind,

        String domain
) {}

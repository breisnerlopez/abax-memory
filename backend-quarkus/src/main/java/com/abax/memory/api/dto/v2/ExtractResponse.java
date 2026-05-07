package com.abax.memory.api.dto.v2;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Response DTO for POST /api/v2/memories/extract — entity extraction.
 *
 * <p>New in v2.1.0: {@code source} indicates the AI provider,
 * {@code extractionTimeMs} reports processing latency.</p>
 *
 * <p>References: HU-005.8.1, FT-V21-001.4</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExtractResponse(
        List<ExtractedEntityDto> entities,
        String source,
        Long extractionTimeMs
) {

    public record ExtractedEntityDto(
            String name,
            String type,
            double confidence
    ) {}
}

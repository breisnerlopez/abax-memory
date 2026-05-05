package com.abax.memory.api.dto.v2;

import java.util.List;

/**
 * Response DTO for POST /api/v2/memories/extract — entity extraction.
 *
 * <p>References: HU-005.8.1</p>
 */
public record ExtractResponse(
        List<ExtractedEntityDto> entities
) {

    public record ExtractedEntityDto(
            String name,
            String type,
            double confidence
    ) {}
}

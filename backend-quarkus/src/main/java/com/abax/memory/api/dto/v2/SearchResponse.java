package com.abax.memory.api.dto.v2;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for paginated, filtered memory listing (GET /api/v2/memories).
 *
 * <p>{@code facets} provides aggregated counts by kind, lifecycle state,
 * and sensitivity level to populate UI filter controls.</p>
 *
 * <p>References: HU-004.5.1, API Design §7.2</p>
 */
public record SearchResponse(
        List<MemoryResponse> items,
        long total,
        int page,
        int size,
        Map<String, Map<String, Long>> facets
) {
}

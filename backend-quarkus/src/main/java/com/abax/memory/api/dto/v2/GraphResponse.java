package com.abax.memory.api.dto.v2;

import java.util.List;

/**
 * Response DTO for graph expansion — includes the center node,
 * all related nodes, and the edges connecting them.
 *
 * <p>References: EP-005, HU-005.7.1</p>
 */
public record GraphResponse(
        MemoryResponse centerNode,
        List<GraphEdge> relations,
        List<MemoryResponse> nodes
) {
}

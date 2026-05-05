package com.abax.memory.api.dto.v2;

import java.util.Map;
import java.util.UUID;

/**
 * A single directed edge in the memory relationship graph.
 *
 * <p>References: EP-005, HU-005.7.1</p>
 */
public record GraphEdge(
        UUID sourceId,
        UUID targetId,
        String relationType,
        Map<String, Object> metadata
) {
}

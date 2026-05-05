package com.abax.memory.domain.model;

import com.abax.memory.domain.enums.RelationType;

import java.util.UUID;

/**
 * Represents a relation inferred by the LLM between two memory fragments.
 *
 * <p>References: FT-005.05</p>
 */
public record InferredRelation(
        /** Source memory fragment ID. */
        UUID sourceId,

        /** Target memory fragment ID. */
        UUID targetId,

        /** Relation type inferred by the LLM. */
        RelationType relationType,

        /** Confidence score [0.0, 1.0] for this inference. */
        double confidence,

        /** Brief explanation of why this relation was inferred. */
        String evidence
) {}

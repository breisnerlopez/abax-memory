package com.abax.memory.domain.model;

/**
 * Result of cross-encoder reranking — maps a memory ID to its scores
 * across the two-stage pipeline.
 *
 * <p>Used by {@link com.abax.memory.domain.service.CrossEncoderService}
 * to return reordered results from the Stage 2 reranker.</p>
 *
 * <p>References: ADR-001, FT-V21-001.1</p>
 */
public record RerankedHit(
        /** Memory fragment point ID (UUID string from Qdrant). */
        String memoryId,

        /** Original cosine-similarity score from dense retrieval (Stage 1). */
        double semanticScore,

        /** Entailment score from the cross-encoder (Stage 2). May be 0.0 if degraded. */
        double crossEncoderScore,

        /** Final composite score used for ranking. */
        double finalScore
) {}

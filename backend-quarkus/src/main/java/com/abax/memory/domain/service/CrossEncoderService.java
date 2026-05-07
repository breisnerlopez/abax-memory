package com.abax.memory.domain.service;

import com.abax.memory.domain.model.RerankedHit;

import java.util.List;

/**
 * Service contract for cross-encoder reranking — Stage 2 of the
 * two-stage search pipeline.
 *
 * <p>Evaluates pairs of (query, document) using an entailment model
 * to produce fine-grained relevance scores, reordering the top-N
 * candidates from the dense retrieval (Stage 1).</p>
 *
 * <p>Reference: ADR-001, FT-V21-001.1</p>
 */
public interface CrossEncoderService {

    /**
     * Reranks a list of candidate documents using a cross-encoder model.
     *
     * @param query      the original search query text
     * @param candidates pairs of (memoryId, documentContent) with their
     *                   semantic scores from Stage 1
     * @param topK       maximum number of results to return after reranking
     * @return reranked list ordered by cross-encoder score (descending),
     *         or empty list if no candidates or uncorrectable error
     */
    List<RerankedHit> rerank(String query, List<CandidateDocument> candidates, int topK);

    /**
     * A candidate document entering the cross-encoder pipeline.
     *
     * @param memoryId      point ID / memory fragment UUID string
     * @param content       the document text to evaluate against the query
     * @param semanticScore cosine-similarity score from dense retrieval
     */
    record CandidateDocument(String memoryId, String content, double semanticScore) {}
}

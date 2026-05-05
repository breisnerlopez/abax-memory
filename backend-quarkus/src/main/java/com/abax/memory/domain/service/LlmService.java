package com.abax.memory.domain.service;

import com.abax.memory.domain.enums.MemoryKind;
import com.abax.memory.domain.model.*;

import java.util.List;

/**
 * Service contract for LLM-powered memory enrichment — v2.0.0.
 * <p>
 * Provides AI capabilities: entity extraction, relation inference,
 * summary generation, validation, and confidence estimation.
 * </p>
 *
 * <p>References: EP-001, FT-001.04, FT-005.05, BR-006</p>
 */
public interface LlmService {

    /**
     * Extracts named entities from the given content.
     *
     * @param content the memory content to analyze
     * @param kind    the memory kind (helps contextualize extraction)
     * @return list of extracted entities (empty if none found)
     */
    List<ExtractedEntity> extractEntities(String content, MemoryKind kind);

    /**
     * Infers potential relations between a fragment and candidate targets.
     *
     * @param fragment   the source memory fragment
     * @param candidates candidate target fragments to evaluate
     * @return list of inferred relations (empty if none inferred)
     */
    List<InferredRelation> inferRelations(MemoryFragment fragment, List<MemoryFragment> candidates);

    /**
     * Generates a concise summary (2-3 sentences) for the given content.
     *
     * @param content the content to summarize
     * @param kind    the memory kind
     * @return a concise summary string
     */
    String generateSummary(String content, MemoryKind kind);

    /**
     * Validates a memory fragment for coherence, duplicates, and lifecycle suggestions.
     *
     * @param fragment the memory fragment to validate
     * @return validation result with issues and suggestions
     */
    ValidationResult validateMemory(MemoryFragment fragment);

    /**
     * Estimates a confidence score [0.0, 1.0] for the completeness and
     * coherence of the memory content.
     *
     * @param content the content to evaluate
     * @param kind    the memory kind
     * @return confidence score in [0.0, 1.0]
     */
    float estimateConfidence(String content, MemoryKind kind);
}

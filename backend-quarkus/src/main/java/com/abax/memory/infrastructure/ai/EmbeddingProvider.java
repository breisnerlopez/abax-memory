package com.abax.memory.infrastructure.ai;

import java.util.List;

/**
 * Port for text-embedding generation — v2.0.0.
 * <p>
 * Abstracts the embedding provider (OpenAI, Cohere, local model, etc.)
 * behind a single-method contract that the rest of the system depends on.
 * </p>
 *
 * <p>References: ADR-004, FT-005.07</p>
 */
public interface EmbeddingProvider {

    /**
     * Generates a dense vector embedding for the given text.
     *
     * @param text input text to embed (not null, not blank)
     * @return float array of the configured dimension (default 3072)
     */
    float[] embed(String text);

    /**
     * Generates embeddings for a batch of texts in a single API call.
     *
     * @param texts input texts (not null, each not blank)
     * @return list of float arrays, one per input text, in the same order
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * Returns the dimensionality of the vectors produced by this provider.
     */
    int dimension();
}

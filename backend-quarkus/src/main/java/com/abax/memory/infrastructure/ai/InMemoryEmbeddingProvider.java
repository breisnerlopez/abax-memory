package com.abax.memory.infrastructure.ai;

import jakarta.enterprise.context.ApplicationScoped;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic in-memory stub for {@link EmbeddingProvider}.
 *
 * <p><strong>MOCK: OpenAI API key is not available in this build
 * environment. This stub generates a reproducible 64-dimensional
 * pseudo-embedding from the SHA-256 hash of the input text.
 * It is NOT suitable for semantic search quality evaluation.</strong></p>
 *
 * <p><strong>REPLACE_BEFORE_PROD: swap this bean with
 * {@code OpenAiEmbeddingProvider} that calls OpenAI text-embedding-3-large
 * API and returns real 3072-dimensional embeddings.</strong></p>
 */
// MOCK: OpenAI API key no disponible en entorno de build
// REPLACE_BEFORE_PROD: conectar a OpenAI text-embedding-3-large real
@ApplicationScoped
public class InMemoryEmbeddingProvider implements EmbeddingProvider {

    private static final int MOCK_DIMENSION = 64;
    private static final float NORMALIZER = (float) Math.sqrt(MOCK_DIMENSION);

    private final MessageDigest digest;

    public InMemoryEmbeddingProvider() {
        try {
            this.digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    @Override
    public float[] embed(String text) {
        byte[] hash = digest.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        float[] vector = new float[MOCK_DIMENSION];
        for (int i = 0; i < MOCK_DIMENSION; i++) {
            // Spread hash bytes across the vector dimensions
            int b0 = hash[i % hash.length] & 0xFF;
            int b1 = hash[(i + 7) % hash.length] & 0xFF;
            vector[i] = ((b0 * 256 + b1) / 65535.0f * 2.0f - 1.0f) / NORMALIZER;
        }
        return vector;
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> results = new ArrayList<>(texts.size());
        for (String text : texts) {
            results.add(embed(text));
        }
        return results;
    }

    @Override
    public int dimension() {
        return MOCK_DIMENSION;
    }
}

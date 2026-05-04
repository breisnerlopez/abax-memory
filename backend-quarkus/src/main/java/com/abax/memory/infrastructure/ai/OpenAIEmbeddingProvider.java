package com.abax.memory.infrastructure.ai;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Real OpenAI embedding provider using LangChain4j — v2.0.0.
 * <p>
 * Delegates to the CDI {@link EmbeddingModel} bean produced by
 * {@code OpenAiConfigProducer} (btl module) which is configured
 * with {@code text-embedding-3-large} and 3072 dimensions.
 * </p>
 *
 * <p><strong>REAL INTEGRATION.</strong> This is not a mock.
 * Requires {@code OPENAI_API_KEY} environment variable or
 * {@code quarkus.langchain4j.openai.api-key} property.</p>
 *
 * <p>References: ADR-004, FT-005.01, FT-005.07</p>
 */
public class OpenAIEmbeddingProvider implements EmbeddingProvider {

    private static final Logger LOG = Logger.getLogger(OpenAIEmbeddingProvider.class);

    public static final int DIMENSION = 3072;

    private final EmbeddingModel embeddingModel;

    /**
     * Constructor receiving the langchain4j EmbeddingModel bean.
     * Injected by CDI via {@code InfrastructureConfig}.
     */
    public OpenAIEmbeddingProvider(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        LOG.infov("OpenAIEmbeddingProvider initialized: model=text-embedding-3-large, dimensions={0}",
                DIMENSION);
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            LOG.warn("embed() called with blank text — returning zero vector");
            return new float[DIMENSION];
        }
        try {
            Response<Embedding> response = embeddingModel.embed(text);
            float[] vector = response.content().vector();
            LOG.debugv("Embedding generated: text_length={0}, vector_dim={1}", text.length(), vector.length);
            return vector;
        } catch (Exception e) {
            LOG.errorv(e, "Failed to generate embedding for text of length {0}", text.length());
            throw new RuntimeException("OpenAI embedding failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        LOG.debugv("Batch embedding requested: count={0}", texts.size());
        try {
            List<TextSegment> segments = texts.stream()
                    .map(TextSegment::from)
                    .collect(Collectors.toList());
            Response<List<Embedding>> response = embeddingModel.embedAll(segments);
            List<float[]> results = new ArrayList<>(texts.size());
            for (Embedding embedding : response.content()) {
                results.add(embedding.vector());
            }
            LOG.infov("Batch embedding completed: count={0}", results.size());
            return results;
        } catch (Exception e) {
            LOG.errorv(e, "Failed to generate batch embeddings: count={0}", texts.size());
            throw new RuntimeException("OpenAI batch embedding failed: " + e.getMessage(), e);
        }
    }

    @Override
    public int dimension() {
        return DIMENSION;
    }
}

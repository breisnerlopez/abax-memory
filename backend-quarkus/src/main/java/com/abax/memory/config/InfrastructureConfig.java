package com.abax.memory.config;

import com.abax.memory.infrastructure.ai.EmbeddingProvider;
import com.abax.memory.infrastructure.ai.InMemoryEmbeddingProvider;
import com.abax.memory.infrastructure.qdrant.InMemoryQdrantClient;
import com.abax.memory.infrastructure.qdrant.QdrantClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * CDI producers for infrastructure dependencies — v2.0.0.
 *
 * <p>Currently wires in-memory stubs for both Qdrant and OpenAI
 * embedding because the real services are not available in this
 * build environment. Both stubs carry the {@code // MOCK: ... // REPLACE_BEFORE_PROD}
 * marker as required by the project's anti-mock policy.</p>
 *
 * <p>When credentials/services become available, replace these
 * producers with real adapter implementations.</p>
 */
@ApplicationScoped
public class InfrastructureConfig {

    // ── Qdrant configuration ──────────────────────────────────────

    @ConfigProperty(name = "abax.v2.qdrant.host", defaultValue = "localhost")
    String qdrantHost;

    @ConfigProperty(name = "abax.v2.qdrant.port", defaultValue = "6333")
    int qdrantPort;

    @ConfigProperty(name = "abax.v2.qdrant.collection", defaultValue = "abax-memories-v2")
    String qdrantCollection;

    @ConfigProperty(name = "abax.v2.qdrant.vector-size", defaultValue = "3072")
    int qdrantVectorSize;

    @ConfigProperty(name = "abax.v2.qdrant.use-tls", defaultValue = "false")
    boolean qdrantUseTls;

    @ConfigProperty(name = "abax.v2.qdrant.api-key")
    String qdrantApiKey;

    // ── OpenAI embedding configuration ────────────────────────────

    @ConfigProperty(name = "abax.v2.openai.api-key")
    String openaiApiKey;

    @ConfigProperty(name = "abax.v2.openai.embedding-model", defaultValue = "text-embedding-3-large")
    String openaiEmbeddingModel;

    @ConfigProperty(name = "abax.v2.openai.embedding-dimensions", defaultValue = "3072")
    int openaiEmbeddingDimensions;

    @ConfigProperty(name = "abax.v2.openai.base-url", defaultValue = "https://api.openai.com/v1")
    String openaiBaseUrl;

    @ConfigProperty(name = "abax.v2.openai.timeout-seconds", defaultValue = "90")
    int openaiTimeoutSeconds;

    /**
     * Produces a {@link QdrantClient} bean.
     * <p>
     * Currently uses {@link InMemoryQdrantClient} — replace with
     * real REST/gRPC client before production deployment.
     * </p>
     */
    @Produces
    @Singleton
    public QdrantClient qdrantClient() {
        // MOCK: Qdrant no disponible en entorno de build
        // REPLACE_BEFORE_PROD: return new QdrantRestClient(qdrantHost, qdrantPort, ...)
        return new InMemoryQdrantClient();
    }

    /**
     * Produces an {@link EmbeddingProvider} bean.
     * <p>
     * Currently uses {@link InMemoryEmbeddingProvider} — replace with
     * OpenAI adapter before production deployment.
     * </p>
     */
    @Produces
    @Singleton
    public EmbeddingProvider embeddingProvider() {
        // MOCK: OpenAI API key no disponible en entorno de build
        // REPLACE_BEFORE_PROD: return new OpenAiEmbeddingProvider(openaiApiKey, openaiEmbeddingModel, ...)
        return new InMemoryEmbeddingProvider();
    }
}

package com.abax.memory.config;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Configuration holder for infrastructure dependencies — v2.0.0.
 *
 * <p>Bean resolution for {@code QdrantClient} and {@code EmbeddingProvider}
 * is handled directly via the {@code @ApplicationScoped} annotation on
 * the in-memory stub classes ({@code InMemoryQdrantClient},
 * {@code InMemoryEmbeddingProvider}).</p>
 *
 * <p>When real services become available, the new adapter classes
 * should also use {@code @ApplicationScoped} (or appropriate scope)
 * to be picked up by CDI.</p>
 *
 * <p>Config properties are retained for use by future real adapters.</p>
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
}

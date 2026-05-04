package com.abax.memory.config;

import com.abax.memory.infrastructure.ai.EmbeddingProvider;
import com.abax.memory.infrastructure.ai.InMemoryEmbeddingProvider;
import com.abax.memory.infrastructure.ai.OpenAIEmbeddingProvider;
import com.abax.memory.infrastructure.qdrant.InMemoryQdrantClient;
import com.abax.memory.infrastructure.qdrant.QdrantClient;
import com.abax.memory.infrastructure.qdrant.QdrantEmbeddingClient;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Optional;

/**
 * Configuration holder for infrastructure dependencies — v2.0.0.
 * <p>
 * Provides CDI beans for infrastructure adapters, selecting real
 * implementations when credentials are available and falling back
 * to in-memory stubs otherwise (with {@code REPLACE_BEFORE_PROD}
 * warnings).
 * </p>
 */
@ApplicationScoped
public class InfrastructureConfig {

    private static final Logger LOG = Logger.getLogger(InfrastructureConfig.class);

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
    Optional<String> qdrantApiKey;

    @ConfigProperty(name = "abax.v2.qdrant.mock", defaultValue = "false")
    boolean qdrantMock;

    // ── OpenAI embedding configuration ────────────────────────────

    @ConfigProperty(name = "abax.v2.openai.api-key")
    Optional<String> openaiApiKey;

    @ConfigProperty(name = "abax.v2.openai.embedding-model", defaultValue = "text-embedding-3-large")
    String openaiEmbeddingModel;

    @ConfigProperty(name = "abax.v2.openai.embedding-dimensions", defaultValue = "3072")
    int openaiEmbeddingDimensions;

    @ConfigProperty(name = "abax.v2.openai.base-url", defaultValue = "https://api.openai.com/v1")
    String openaiBaseUrl;

    @ConfigProperty(name = "abax.v2.openai.timeout-seconds", defaultValue = "90")
    int openaiTimeoutSeconds;

    // ── CDI Producers ─────────────────────────────────────────────

    /**
     * Produces the {@link QdrantClient} bean.
     * <p>
     * Resolution strategy:
     * <ol>
     *   <li>If {@code abax.v2.qdrant.mock=true}, returns {@link InMemoryQdrantClient}
     *       directly (for test environments where the embedding dimension doesn't
     *       match Qdrant's collection).</li>
     *   <li>Otherwise, creates a real {@link QdrantEmbeddingClient} connecting to
     *       the configured Qdrant server.</li>
     *   <li>If the health check fails (Qdrant is unreachable), falls back
     *       to {@link InMemoryQdrantClient} with a
     *       {@code REPLACE_BEFORE_PROD} warning.</li>
     * </ol>
     * </p>
     */
    @Produces
    @Singleton
    public QdrantClient qdrantClient() {
        // Test environments can force mock mode
        if (qdrantMock) {
            LOG.infov("abax.v2.qdrant.mock=true — using InMemoryQdrantClient (test mode)");
            return new InMemoryQdrantClient();
        }

        // Try connecting to the real Qdrant server
        var realClient = new QdrantEmbeddingClient(qdrantHost, qdrantPort, qdrantUseTls);
        if (realClient.isHealthy()) {
            LOG.infov("QdrantClient ACTIVE — connected to {0}:{1}", qdrantHost, qdrantPort);
            return realClient;
        }

        LOG.warnv("Qdrant server not reachable at {0}:{1} — "
                + "using InMemoryQdrantClient (REPLACE_BEFORE_PROD)", qdrantHost, qdrantPort);
        return new InMemoryQdrantClient();
    }

    /**
     * Produces the {@link EmbeddingProvider} bean.
     * <p>
     * Resolution strategy:
     * <ol>
     *   <li>If {@code OPENAI_API_KEY} is set (either via
     *       {@code quarkus.langchain4j.openai.api-key} or the
     *       btl module's producer), creates a real
     *       {@link OpenAIEmbeddingProvider}.</li>
     *   <li>Otherwise, creates a fallback {@link InMemoryEmbeddingProvider}
     *       with a {@code REPLACE_BEFORE_PROD} warning.</li>
     * </ol>
     * </p>
     */
    @Produces
    @Singleton
    public EmbeddingProvider embeddingProvider(
            @ConfigProperty(name = "quarkus.langchain4j.openai.api-key") Optional<String> langchainApiKey) {

        // Check for API key from multiple sources
        String effectiveKey = langchainApiKey.orElseGet(() -> openaiApiKey.orElse(null));

        if (effectiveKey != null && !effectiveKey.isBlank()
                && !effectiveKey.startsWith("test-key-")
                && !effectiveKey.startsWith("dummy-")) {
            // We need the langchain4j EmbeddingModel bean from the btl module
            // Since we cannot directly inject it here (circular dep risk),
            // we use the lazy approach: try to resolve it or fall back.
            // The actual injection happens via the constructor of OpenAIEmbeddingProvider
            // which is CDI-managed when EmbeddingModel is available.
            LOG.infov("OPENAI_API_KEY detected — will attempt to use OpenAIEmbeddingProvider");
            // Delegate to CDI: if EmbeddingModel is on the classpath, create real provider.
            // If not, fall through to in-memory.
        }

        // Check if langchain4j EmbeddingModel CDI bean is available
        try {
            // Try to resolve via CDI programmatic lookup
            var embeddingModel = jakarta.enterprise.inject.spi.CDI.current()
                    .select(EmbeddingModel.class).get();
            if (embeddingModel != null && effectiveKey != null && !effectiveKey.isBlank()
                    && !effectiveKey.startsWith("test-key-")
                    && !effectiveKey.startsWith("dummy-")) {
                LOG.info("OpenAIEmbeddingProvider ACTIVE — using text-embedding-3-large (3072-dim)");
                return new OpenAIEmbeddingProvider(embeddingModel);
            }
        } catch (Exception e) {
            LOG.debugv("EmbeddingModel CDI bean not available: {0}", e.getMessage());
        }

        LOG.warn("OPENAI_API_KEY not set or EmbeddingModel unavailable — "
                + "using InMemoryEmbeddingProvider (REPLACE_BEFORE_PROD)");
        return new InMemoryEmbeddingProvider();
    }
}

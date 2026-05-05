package com.abax.memory.infrastructure.ai;

import com.abax.memory.infrastructure.qdrant.QdrantClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test that validates the full embedding cycle using
 * OpenAI API and Qdrant — v2.0.0.
 *
 * <p><strong>Preconditions:</strong>
 * <ul>
 *   <li>{@code OPENAI_API_KEY} environment variable must be set with a valid key.</li>
 *   <li>Qdrant must be running at {@code localhost:6333} with collection
 *       {@code abax-memories-v2} configured for 3072-dim Cosine vectors.</li>
 * </ul>
 * </p>
 *
 * <p>This test is automatically skipped (via {@code Assumptions}) when
 * the OpenAI API key is not configured or the real
 * {@link OpenAIEmbeddingProvider} is not active.</p>
 */
@QuarkusTest
@TestProfile(OpenAiE2ETest.E2ETestProfile.class)
@DisplayName("OpenAI + Qdrant end-to-end embedding cycle")
class OpenAiE2ETest {

    private static final Logger LOG = Logger.getLogger(OpenAiE2ETest.class);

    private static final String COLLECTION = "abax-memories-v2";

    @Inject
    EmbeddingProvider embeddingProvider;

    @Inject
    QdrantClient qdrantClient;

    /**
     * Custom test profile that overrides the default test
     * {@code application.properties} to enable real OpenAI and
     * real Qdrant connections (disabling in-memory mocks).
     */
    public static class E2ETestProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> config = new HashMap<>();

            // H2 compatibility — same as H2TestProfile:
            // disable Flyway (PostgreSQL-specific migrations) and
            // use Hibernate drop-and-create for the test schema
            config.put("quarkus.flyway.migrate-at-start", "false");
            config.put("quarkus.hibernate-orm.database.generation", "drop-and-create");

            // Disable in-memory mocks so we hit real services
            config.put("abax.v2.qdrant.mock", "false");
            config.put("abax.v2.llm.mock", "false");

            // Bridge the OPENAI_API_KEY env var into the Quarkus config
            // property that InfrastructureConfig.embeddingProvider() reads
            String apiKey = System.getenv("OPENAI_API_KEY");
            if (apiKey != null && !apiKey.isBlank()) {
                config.put("abax.v2.openai.api-key", apiKey);
            }

            // Ensure langchain4j test key doesn't block the real key
            String realKey = config.get("abax.v2.openai.api-key");
            if (realKey != null && !realKey.isBlank()) {
                config.put("quarkus.langchain4j.openai.api-key", realKey);
            }

            return config;
        }
    }

    @Test
    @DisplayName("Embed text via OpenAI → store in Qdrant → search → verify score > 0.9")
    void testEmbedAndSearch() {
        // Skip silently if the real OpenAIEmbeddingProvider is not active
        // (no API key configured, EmbeddingModel unavailable, etc.)
        Assumptions.assumeTrue(
                embeddingProvider instanceof OpenAIEmbeddingProvider,
                "Skipping: OpenAI API key not configured — "
                        + "InMemoryEmbeddingProvider is active instead of OpenAIEmbeddingProvider"
        );

        LOG.infov("OpenAIEmbeddingProvider ACTIVE — dimension={0}", embeddingProvider.dimension());

        // ── Step 1: Generate embedding via OpenAI ─────────────────
        String text = "The database migration failed due to a version mismatch";
        float[] embedding = embeddingProvider.embed(text);

        assertThat(embedding).isNotNull();
        assertThat(embedding.length).isEqualTo(3072);

        LOG.infov("Embedding generated: input_length={0}, vector_dim={1}",
                text.length(), embedding.length);

        // ── Step 2: Store in Qdrant ──────────────────────────────
        String pointId = UUID.randomUUID().toString();
        Map<String, Object> payload = Map.of(
                "text", text,
                "kind", "E2E_TEST",
                "test_run", UUID.randomUUID().toString()
        );

        qdrantClient.upsert(COLLECTION, pointId, embedding, payload);
        LOG.infov("Point stored in Qdrant: collection={0}, pointId={1}", COLLECTION, pointId);

        // ── Step 3: Search by embedding ──────────────────────────
        List<QdrantClient.ScoredHit> results = qdrantClient.search(
                COLLECTION, embedding, Map.of(), 3);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).score()).isGreaterThan(0.9f);

        LOG.infov("E2E test PASSED: top_score={0}, pointId={1}",
                results.get(0).score(), results.get(0).pointId());
    }
}

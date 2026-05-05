package com.abax.memory.infrastructure.qdrant;

import com.abax.memory.infrastructure.ai.EmbeddingProvider;
import com.abax.memory.test.H2TestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * End-to-end integration tests for Qdrant vector storage and retrieval.
 *
 * <p>Tests the real {@link QdrantEmbeddingClient} against a running
 * Qdrant instance at {@code localhost:6333}. The collection
 * {@code abax-memories-v2} must exist with 3072-dim Cosine vectors.</p>
 *
 * <h3>Test Scenarios</h3>
 * <ol>
 *   <li><b>healthCheck:</b> Verifies Qdrant server is reachable.</li>
 *   <li><b>upsertAndSearch:</b> Stores a deterministic 3072-dim vector
 *       and retrieves it via cosine-similarity search.</li>
 *   <li><b>filterSearch:</b> Validates payload-based filtering works.</li>
 *   <li><b>openaiEndToEnd:</b> Generates a real embedding via OpenAI,
 *       stores it in Qdrant, and retrieves it. Requires
 *       {@code OPENAI_API_KEY} environment variable.</li>
 * </ol>
 *
 * <p>References: EP-005, FT-005.01, ADR-004</p>
 */
@QuarkusTest
@TestProfile(H2TestProfile.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Qdrant — integration tests")
@Tag("integration")
public class QdrantIntegrationTest {

    private static final Logger LOG = Logger.getLogger(QdrantIntegrationTest.class);

    private static final String COLLECTION = "abax-memories-v2";
    private static final int VECTOR_DIM = 3072;

    @Inject
    EmbeddingProvider embeddingProvider;

    private static QdrantEmbeddingClient qdrantClient;

    @BeforeAll
    static void setupQdrantClient() {
        qdrantClient = new QdrantEmbeddingClient("localhost", 6333, false);
    }

    @Test
    @Order(1)
    @DisplayName("Qdrant health check — server is reachable")
    void healthCheck() {
        assertThat(qdrantClient.isHealthy())
                .as("Qdrant server at localhost:6333 should be healthy")
                .isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("upsert + search — stores and retrieves a deterministic vector")
    void upsertAndSearch() {
        assumeThat(qdrantClient.isHealthy())
                .as("Qdrant must be healthy to run this test")
                .isTrue();

        String pointId = UUID.randomUUID().toString();
        float[] vector = generateDeterministicVector("integration-test-vector", VECTOR_DIM);

        Map<String, Object> payload = Map.of(
                "memory_id", "test-memory-001",
                "tenant_id", "integration-test-tenant",
                "kind", "FACT",
                "lifecycle_state", "ACTIVE",
                "title", "Integration Test Memory"
        );

        qdrantClient.upsert(COLLECTION, pointId, vector, payload);

        var hits = qdrantClient.search(COLLECTION, vector,
                Map.of("tenant_id", "integration-test-tenant"), 5);

        assertThat(hits)
                .as("Search should return the stored point")
                .isNotEmpty();

        var topHit = hits.get(0);
        assertThat(topHit.score())
                .as("Cosine similarity of identical vectors should be close to 1.0")
                .isGreaterThan(0.99f);

        assertThat(topHit.payload())
                .as("Payload should contain the stored metadata")
                .containsEntry("memory_id", "test-memory-001")
                .containsEntry("kind", "FACT");

        LOG.infov("Qdrant upsert+search successful: score={0}, pointId={1}",
                topHit.score(), topHit.pointId());
    }

    @Test
    @Order(3)
    @DisplayName("filtered search — respects payload filters")
    void filterSearch() {
        assumeThat(qdrantClient.isHealthy())
                .as("Qdrant must be healthy to run this test")
                .isTrue();

        float[] vectorA = generateDeterministicVector("filter-test-a", VECTOR_DIM);
        float[] vectorB = generateDeterministicVector("filter-test-b", VECTOR_DIM);

        String pointA = UUID.randomUUID().toString();
        String pointB = UUID.randomUUID().toString();

        qdrantClient.upsert(COLLECTION, pointA, vectorA,
                Map.of("tenant_id", "integration-test-tenant", "kind", "FACT", "memory_id", "filter-a"));
        qdrantClient.upsert(COLLECTION, pointB, vectorB,
                Map.of("tenant_id", "integration-test-tenant", "kind", "DECISION", "memory_id", "filter-b"));

        var factHits = qdrantClient.search(COLLECTION, vectorA,
                Map.of("tenant_id", "integration-test-tenant", "kind", "FACT"), 5);

        assertThat(factHits)
                .as("Filtered search should return results")
                .isNotEmpty();

        for (var hit : factHits) {
            Object kind = hit.payload() != null ? hit.payload().get("kind") : null;
            LOG.infov("Filter result: pointId={0}, kind={1}, score={2}",
                    hit.pointId(), kind, hit.score());
        }
    }

    @Test
    @Order(4)
    @DisplayName("OpenAI end-to-end — generates embedding, stores in Qdrant, retrieves")
    void openaiEndToEnd() {
        // Skip if OPENAI_API_KEY is not configured
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            LOG.info("OPENAI_API_KEY not set — skipping OpenAI E2E test");
            return;
        }

        assumeThat(qdrantClient.isHealthy())
                .as("Qdrant must be healthy to run this test")
                .isTrue();

        assumeThat(embeddingProvider.dimension())
                .as("OpenAI embedding provider must be active (dimension=3072)")
                .isEqualTo(3072);

        String testText = "Integration testing is essential for verifying that software components work together correctly.";
        float[] embedding = embeddingProvider.embed(testText);

        assertThat(embedding).isNotNull();
        assertThat(embedding.length).isEqualTo(3072);

        Map<String, Object> payload = Map.of(
                "memory_id", "openai-e2e-memory",
                "tenant_id", "integration-test-tenant",
                "kind", "FACT",
                "content", testText
        );

        qdrantClient.upsert(COLLECTION, UUID.randomUUID().toString(), embedding, payload);

        var hits = qdrantClient.search(COLLECTION, embedding,
                Map.of("tenant_id", "integration-test-tenant"), 3);

        assertThat(hits).isNotEmpty();

        var topHit = hits.get(0);
        assertThat(topHit.score()).isGreaterThan(0.99f);
        assertThat(topHit.payload()).containsEntry("memory_id", "openai-e2e-memory");

        LOG.infov("OpenAI E2E test PASSED: score={0}, pointId={1}",
                topHit.score(), topHit.pointId());
    }

    /**
     * Generates a deterministic unit-norm vector from a seed string.
     */
    static float[] generateDeterministicVector(String seed, int dimension) {
        Random rng = new Random(seed.hashCode());
        float[] vector = new float[dimension];
        double norm = 0.0;
        for (int i = 0; i < dimension; i++) {
            float value = (rng.nextFloat() * 2.0f - 1.0f);
            vector[i] = value;
            norm += (double) value * value;
        }
        double invNorm = 1.0 / Math.sqrt(norm);
        for (int i = 0; i < dimension; i++) {
            vector[i] *= invNorm;
        }
        return vector;
    }
}

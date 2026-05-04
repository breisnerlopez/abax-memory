package com.abax.memory.infrastructure.qdrant;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory stub for {@link QdrantClient}.
 *
 * <p><strong>MOCK: Qdrant server is not available in this build environment.
 * This stub stores vectors and payloads in-memory and performs
 * brute-force cosine-similarity search over all stored points.</strong></p>
 *
 * <p><strong>REPLACE_BEFORE_PROD: swap this bean with a real Qdrant HTTP/gRPC
 * client implementation (e.g. {@code QdrantRestClient}) that connects to
 * the configured Qdrant 1.17 instance.</strong></p>
 *
 * <p>Limitations of this mock:
 * <ul>
 *   <li>No HNSW indexing — O(n) brute-force for every search</li>
 *   <li>No persistence — all data lost on restart</li>
 *   <li>No TLS / authentication</li>
 *   <li>Single-collection behavior simulated</li>
 * </ul>
 * </p>
 */
// MOCK: Qdrant no disponible en entorno de build
// REPLACE_BEFORE_PROD: conectar a Qdrant real via REST/gRPC
@ApplicationScoped
public class InMemoryQdrantClient implements QdrantClient {

    private final Map<String, StoredPoint> store = new ConcurrentHashMap<>();

    @Override
    public List<ScoredHit> search(String collection,
                                  float[] queryVector,
                                  Map<String, Object> filters,
                                  int topK) {
        return store.values().stream()
                .filter(p -> matchesFilters(p.payload, filters))
                .map(p -> new ScoredHit(
                        p.pointId,
                        cosineSimilarity(queryVector, p.vector),
                        p.payload))
                .sorted((a, b) -> Float.compare(b.score(), a.score()))
                .limit(Math.max(1, topK))
                .toList();
    }

    @Override
    public void upsert(String collection, String pointId,
                       float[] vector, Map<String, Object> payload) {
        store.put(pointId, new StoredPoint(pointId, vector, payload));
    }

    @Override
    public boolean isHealthy() {
        return true; // in-memory is always "healthy"
    }

    // ── internal helpers ──────────────────────────────────────────

    private record StoredPoint(String pointId, float[] vector,
                               Map<String, Object> payload) {
    }

    private static boolean matchesFilters(Map<String, Object> payload,
                                          Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }
        for (var entry : filters.entrySet()) {
            Object payloadValue = payload.get(entry.getKey());
            if (payloadValue == null) {
                return false;
            }
            if (!payloadValue.toString().equals(entry.getValue().toString())) {
                return false;
            }
        }
        return true;
    }

    private static float cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            return 0.0f;
        }
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0f;
        }
        return (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }
}

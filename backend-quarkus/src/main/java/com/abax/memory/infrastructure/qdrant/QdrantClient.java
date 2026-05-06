package com.abax.memory.infrastructure.qdrant;

import java.util.List;
import java.util.Map;

/**
 * Port for Qdrant vector-database operations — v2.0.0.
 * <p>
 * Abstracts search (cosine-similarity top-K) and upsert
 * (store/update vectors with payload) behind a swappable
 * adapter.
 * </p>
 */
public interface QdrantClient {

    /**
     * Performs a semantic search returning the top-K closest vectors
     * by cosine similarity.
     *
     * @param collection  Qdrant collection name
     * @param queryVector embedding vector of the search query
     * @param filters     payload key-value filters (AND semantics)
     * @param topK        maximum number of results to return
     * @return scored hits ordered by descending similarity
     */
    List<ScoredHit> search(String collection,
                           float[] queryVector,
                           Map<String, Object> filters,
                           int topK);

    /**
     * Inserts or updates a point (vector + payload) in the given collection.
     * Naturally idempotent.
     *
     * @param collection collection name
     * @param pointId    unique point identifier (typically memory fragment id)
     * @param vector     embedding vector
     * @param payload    metadata payload attached to the point
     */
    void upsert(String collection,
                String pointId,
                float[] vector,
                Map<String, Object> payload);

    /**
     * Checks whether the Qdrant server is reachable.
     */
    boolean isHealthy();

    /**
     * Deletes points matching the given payload filters — v2.1.0 FT-V21-004.3.
     *
     * @param collection collection name
     * @param filters    payload key-value filters (AND semantics)
     * @return number of points deleted
     */
    long deleteByFilter(String collection, Map<String, Object> filters);

    /**
     * A single scored result from a Qdrant search.
     */
    record ScoredHit(String pointId, float score, Map<String, Object> payload) {
    }
}

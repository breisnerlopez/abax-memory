package com.btl.administrador.api.integration.qdrant;

import com.btl.administrador.api.dto.SearchFiltersRequest;
import com.btl.administrador.api.exception.ApiException;
import com.btl.administrador.api.service.model.SearchHit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class QdrantEmbeddingService implements SearchIndexer {

    private static final Logger LOG = Logger.getLogger(QdrantEmbeddingService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Inject
    QdrantConfig qdrantConfig;

    @Inject
    HttpClient httpClient;

    @Inject
    EmbeddingModel embeddingModel;

    private boolean initialized = false;

    @PostConstruct
    void ensureCollection() {
        try {
            initCollection();
            initialized = true;
            LOG.infov("Qdrant collection '{0}' ready at {1}", qdrantConfig.getCollection(), qdrantConfig.baseUrl());
        } catch (Exception e) {
            LOG.errorv(e, "Failed to initialize Qdrant collection. Search will be unavailable.");
        }
    }

    @Override
    public void index(String memoryId, String title, String markdown) {
        if (!initialized) {
            LOG.warn("Qdrant not initialized, skipping index for " + memoryId);
            return;
        }

        try {
            String text = title + "\n" + markdown;
            Embedding embedding = embeddingModel.embed(text).content();

            List<Float> vector = toFloatList(embedding.vector());
            if (vector.size() != qdrantConfig.getVectorSize()) {
                LOG.warnv("Embedding dimension mismatch: expected {0}, got {1}. Padding/truncating.",
                        qdrantConfig.getVectorSize(), vector.size());
                vector = normalizeVector(vector, qdrantConfig.getVectorSize());
            }

            ObjectNode payload = OBJECT_MAPPER.createObjectNode();
            payload.put("memory_id", memoryId);
            payload.put("title", title);

            // Qdrant v1.17.1+: vector is a plain float array, no named-vector wrapper
            ObjectNode point = OBJECT_MAPPER.createObjectNode();
            point.put("id", toUUID(memoryId).toString());
            point.set("vector", toArrayNode(vector));
            point.set("payload", payload);

            ObjectNode body = OBJECT_MAPPER.createObjectNode();
            ArrayNode points = OBJECT_MAPPER.createArrayNode();
            points.add(point);
            body.set("points", points);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(qdrantConfig.pointsUri() + "?wait=true"))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                LOG.infov("Indexed memory {0} in Qdrant", memoryId);
            } else {
                LOG.errorv("Qdrant index failed for {0}: HTTP {1} - {2}", memoryId, response.statusCode(), response.body());
                throw new ApiException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                        "QDRANT_INDEX_FAILED", "Failed to index memory in Qdrant");
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.errorv(e, "Qdrant index error for {0}", memoryId);
            throw new ApiException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    "QDRANT_INDEX_ERROR", "Qdrant indexing error: " + e.getMessage());
        }
    }

    @Override
    public List<SearchHit> search(String query, int topK, SearchFiltersRequest filters) {
        if (!initialized) {
            LOG.warn("Qdrant not initialized, returning empty search results");
            return List.of();
        }

        try {
            Embedding embedding = embeddingModel.embed(query).content();
            List<Float> vector = toFloatList(embedding.vector());
            if (vector.size() != qdrantConfig.getVectorSize()) {
                vector = normalizeVector(vector, qdrantConfig.getVectorSize());
            }

            // Qdrant v1.17.1+: vector is a plain float array, no named-vector wrapper
            ObjectNode body = OBJECT_MAPPER.createObjectNode();
            body.set("vector", toArrayNode(vector));
            body.put("limit", topK);
            body.put("with_payload", true);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(qdrantConfig.pointsUri() + "/search"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOG.errorv("Qdrant search failed: HTTP {0} - {1}", response.statusCode(), response.body());
                return List.of();
            }

            JsonNode root = OBJECT_MAPPER.readTree(response.body());
            JsonNode results = root.get("result");
            if (results == null || !results.isArray()) {
                return List.of();
            }

            List<SearchHit> hits = new ArrayList<>();
            for (JsonNode node : results) {
                double score = node.has("score") ? node.get("score").asDouble() : 0.0;
                String memoryId = null;
                // Read memory_id from payload because point id is now a UUID
                if (node.has("payload") && node.get("payload").has("memory_id")) {
                    memoryId = node.get("payload").get("memory_id").asText();
                }
                if (memoryId != null) {
                    hits.add(new SearchHit(memoryId, score));
                }
            }

            LOG.infov("Qdrant search for '{0}' returned {1} hits", query, hits.size());
            return hits;
        } catch (Exception e) {
            LOG.errorv(e, "Qdrant search error for query: {0}", query);
            return List.of();
        }
    }

    @Override
    public void clear() {
        if (!initialized) {
            return;
        }

        try {
            ObjectNode filter = OBJECT_MAPPER.createObjectNode();
            ObjectNode must = OBJECT_MAPPER.createObjectNode();
            filter.set("must", must);
            must.put("match_all", OBJECT_MAPPER.createObjectNode());

            ObjectNode body = OBJECT_MAPPER.createObjectNode();
            body.set("filter", filter);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(qdrantConfig.pointsUri() + "/delete"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                LOG.info("Qdrant collection cleared");
            }
        } catch (Exception e) {
            LOG.errorv(e, "Failed to clear Qdrant collection");
        }
    }

    private void initCollection() throws Exception {
        // Check if collection exists
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(qdrantConfig.collectionUri())
                .GET()
                .build();

        HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());

        if (getResponse.statusCode() == 200) {
            LOG.infov("Qdrant collection '{0}' already exists", qdrantConfig.getCollection());
            return;
        }

        // Qdrant v1.17.1+: vectors config is directly {"size": N, "distance": "Cosine"}
        // without the named-vector wrapper (no empty-string key).
        ObjectNode vectorsConfig = OBJECT_MAPPER.createObjectNode();
        vectorsConfig.put("size", qdrantConfig.getVectorSize());
        vectorsConfig.put("distance", "Cosine");

        ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.set("vectors", vectorsConfig);

        HttpRequest putRequest = HttpRequest.newBuilder()
                .uri(qdrantConfig.collectionUri())
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(body)))
                .build();

        HttpResponse<String> putResponse = httpClient.send(putRequest, HttpResponse.BodyHandlers.ofString());

        if (putResponse.statusCode() >= 200 && putResponse.statusCode() < 300) {
            LOG.infov("Created Qdrant collection '{0}' with vector size {1}", qdrantConfig.getCollection(), qdrantConfig.getVectorSize());
        } else {
            throw new ApiException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    "QDRANT_INIT_FAILED",
                    "Failed to create Qdrant collection: HTTP " + putResponse.statusCode() + " - " + putResponse.body());
        }
    }

    private ArrayNode toArrayNode(List<Float> vector) {
        ArrayNode array = OBJECT_MAPPER.createArrayNode();
        for (Float value : vector) {
            array.add(value.doubleValue());
        }
        return array;
    }

    private List<Float> normalizeVector(List<Float> vector, int targetSize) {
        List<Float> normalized = new ArrayList<>(targetSize);
        for (int i = 0; i < targetSize; i++) {
            if (i < vector.size()) {
                normalized.add(vector.get(i));
            } else {
                normalized.add(0.0f);
            }
        }
        return normalized;
    }

    private List<Float> toFloatList(float[] vector) {
        List<Float> result = new ArrayList<>(vector.length);
        for (float value : vector) {
            result.add(value);
        }
        return result;
    }

    /**
     * Converts a memoryId string (e.g., "MEM-c7543790") to a deterministic UUID.
     * Qdrant v1.17+ only accepts unsigned integer (64-bit) or UUID as point IDs.
     * Using UUID.nameUUIDFromBytes ensures the same memoryId always maps to the same point ID,
     * which is essential for idempotent upserts.
     */
    private UUID toUUID(String memoryId) {
        return UUID.nameUUIDFromBytes(memoryId.getBytes(StandardCharsets.UTF_8));
    }
}

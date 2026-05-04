package com.abax.memory.infrastructure.qdrant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Real Qdrant HTTP client implementation — v2.0.0.
 *
 * <p><strong>REAL INTEGRATION.</strong> Connects to a Qdrant 1.17+ server
 * via REST API. Requires Qdrant to be running and the target collection
 * to exist with matching vector dimensions (3072).</p>
 *
 * <p>Uses {@code java.net.http.HttpClient} for HTTP communication and
 * Jackson for JSON serialization. All operations are synchronous with
 * configurable timeouts.</p>
 *
 * <p>References: ADR-004, EP-005, Qdrant REST API v1.17</p>
 */
public class QdrantEmbeddingClient implements QdrantClient {

    private static final Logger LOG = Logger.getLogger(QdrantEmbeddingClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final String baseUrl;
    private final boolean initialized;

    /**
     * Creates a new Qdrant HTTP client.
     *
     * @param host    Qdrant server hostname
     * @param port    Qdrant server HTTP port
     * @param useTls  whether to use HTTPS
     */
    public QdrantEmbeddingClient(String host, int port, boolean useTls) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        String protocol = useTls ? "https" : "http";
        this.baseUrl = protocol + "://" + host + ":" + port;
        this.initialized = checkHealth();
        if (initialized) {
            LOG.infov("QdrantEmbeddingClient connected to {0}", baseUrl);
        } else {
            LOG.warnv("QdrantEmbeddingClient could not reach Qdrant at {0} — "
                    + "health check failed", baseUrl);
        }
    }

    // ── QdrantClient contract ───────────────────────────────────────

    @Override
    public List<ScoredHit> search(String collection, float[] queryVector,
                                   Map<String, Object> filters, int topK) {
        if (!initialized) {
            LOG.warn("Qdrant search skipped: client not initialized");
            return List.of();
        }

        try {
            ObjectNode body = MAPPER.createObjectNode();
            body.set("vector", toArrayNode(queryVector));
            body.put("limit", topK);
            body.put("with_payload", true);

            // Build Qdrant filter if we have filters beyond the all-match
            if (filters != null && !filters.isEmpty()) {
                ObjectNode filterNode = buildQdrantFilter(filters);
                body.set("filter", filterNode);
            }

            String url = baseUrl + "/collections/" + collection + "/points/search";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(REQUEST_TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LOG.errorv("Qdrant search failed: HTTP {0} — {1}", response.statusCode(), response.body());
                return List.of();
            }

            JsonNode root = MAPPER.readTree(response.body());
            JsonNode results = root.get("result");
            if (results == null || !results.isArray()) {
                return List.of();
            }

            List<ScoredHit> hits = new ArrayList<>();
            for (JsonNode node : results) {
                String pointId = null;
                if (node.has("id")) {
                    JsonNode idNode = node.get("id");
                    pointId = idNode.isTextual() ? idNode.asText() : idNode.toString();
                }
                float score = node.has("score") ? (float) node.get("score").asDouble() : 0.0f;
                Map<String, Object> payload = jsonToMap(node.get("payload"));
                hits.add(new ScoredHit(pointId, score, payload));
            }

            LOG.debugv("Qdrant search: collection={0}, topK={1}, hits={2}", collection, topK, hits.size());
            return hits;
        } catch (Exception e) {
            LOG.errorv(e, "Qdrant search error for collection={0}", collection);
            return List.of();
        }
    }

    @Override
    public void upsert(String collection, String pointId, float[] vector,
                        Map<String, Object> payload) {
        if (!initialized) {
            LOG.warnv("Qdrant upsert skipped: client not initialized. pointId={0}", pointId);
            return;
        }

        try {
            // Qdrant v1.17+ requires point IDs to be UUIDs or unsigned integers.
            // Try to parse as UUID first; if not a valid UUID, generate a deterministic one.
            String uuidPointId;
            try {
                uuidPointId = UUID.fromString(pointId).toString();
            } catch (IllegalArgumentException e) {
                uuidPointId = UUID.nameUUIDFromBytes(pointId.getBytes(StandardCharsets.UTF_8)).toString();
                LOG.debugv("Converted non-UUID pointId '{0}' to UUID: {1}", pointId, uuidPointId);
            }

            ObjectNode point = MAPPER.createObjectNode();
            point.put("id", uuidPointId);
            point.set("vector", toArrayNode(vector));
            point.set("payload", mapToJson(payload));

            ObjectNode body = MAPPER.createObjectNode();
            ArrayNode points = MAPPER.createArrayNode();
            points.add(point);
            body.set("points", points);

            String url = baseUrl + "/collections/" + collection + "/points?wait=true";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(REQUEST_TIMEOUT)
                    .PUT(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                LOG.debugv("Qdrant upsert: pointId={0}, collection={1}", pointId, collection);
            } else {
                LOG.errorv("Qdrant upsert failed for {0}: HTTP {1} — {2}",
                        pointId, response.statusCode(), response.body());
            }
        } catch (Exception e) {
            LOG.errorv(e, "Qdrant upsert error for pointId={0}, collection={1}", pointId, collection);
        }
    }

    @Override
    public boolean isHealthy() {
        return initialized && checkHealth();
    }

    // ── Private helpers ──────────────────────────────────────────────

    private boolean checkHealth() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/healthz"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            LOG.debugv("Qdrant health check failed: {0}", e.getMessage());
            return false;
        }
    }

    /**
     * Builds a Qdrant-compatible filter JSON from a key-value map.
     * Each entry becomes a {@code must} condition with exact match.
     */
    private ObjectNode buildQdrantFilter(Map<String, Object> filters) {
        ObjectNode filterNode = MAPPER.createObjectNode();
        ArrayNode mustArray = MAPPER.createArrayNode();

        for (var entry : filters.entrySet()) {
            ObjectNode condition = MAPPER.createObjectNode();
            condition.put("key", entry.getKey());

            ObjectNode match = MAPPER.createObjectNode();
            match.put("value", entry.getValue() != null ? entry.getValue().toString() : "");
            condition.set("match", match);

            mustArray.add(condition);
        }

        filterNode.set("must", mustArray);
        return filterNode;
    }

    private ArrayNode toArrayNode(float[] vector) {
        ArrayNode array = MAPPER.createArrayNode();
        for (float v : vector) {
            array.add(v);
        }
        return array;
    }

    /**
     * Converts a Jackson JsonNode to a plain Java Map recursively.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonToMap(JsonNode node) {
        if (node == null || node.isNull()) {
            return Collections.emptyMap();
        }
        try {
            return MAPPER.convertValue(node, Map.class);
        } catch (Exception e) {
            LOG.debugv("Failed to convert JSON node to map: {0}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Converts a Map to a Jackson ObjectNode.
     */
    private ObjectNode mapToJson(Map<String, Object> map) {
        ObjectNode node = MAPPER.createObjectNode();
        if (map != null) {
            for (var entry : map.entrySet()) {
                if (entry.getValue() instanceof String s) {
                    node.put(entry.getKey(), s);
                } else if (entry.getValue() instanceof Number n) {
                    node.put(entry.getKey(), n.doubleValue());
                } else if (entry.getValue() instanceof Boolean b) {
                    node.put(entry.getKey(), b);
                } else if (entry.getValue() != null) {
                    node.put(entry.getKey(), entry.getValue().toString());
                }
            }
        }
        return node;
    }
}

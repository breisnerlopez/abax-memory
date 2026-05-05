package com.abax.memory.infrastructure.service;

import com.abax.memory.api.dto.v2.SemanticSearchRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility that translates {@link SemanticSearchRequest} filter fields
 * into a payload key-value map suitable for {@code QdrantClient.search()}
 * AND semantics.
 *
 * <p>Each filter entry in the map acts as an equality constraint.
 * Filters that return an empty map are treated as "match all" by the
 * Qdrant client adapter.</p>
 *
 * <p>References: EP-005, §5.3 of architecture document</p>
 */
public final class SearchFilterBuilder {

    private SearchFilterBuilder() {
        // utility class
    }

    /**
     * Builds a filter map from the given search request.
     * Only sets filters that Qdrant payloads can match.
     * Additional filters (lifecycle state, date range) are applied
     * post-search at the service layer.
     *
     * @param request  the semantic search request
     * @param tenantId tenant scope identifier
     * @return filter map for Qdrant
     */
    public static Map<String, Object> buildQdrantFilters(SemanticSearchRequest request, String tenantId) {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("tenant_id", tenantId);

        // kind filter: memory kind values in payload
        if (request.getKinds() != null && !request.getKinds().isEmpty()) {
            // Qdrant payload stores kind as a string; we pass comma-separated
            // and the in-memory adapter matches on exact string
            filters.put("kind", String.join(",", request.getKinds().stream()
                    .map(Enum::name)
                    .toList()));
        }

        // sensitivity filter: only applies if sensitivityMax is set
        if (request.getSensitivityMax() != null) {
            filters.put("sensitivity_level", request.getSensitivityMax().name());
        }

        return filters;
    }

    /**
     * Builds payload metadata for a Qdrant upsert from a memory fragment's fields.
     *
     * @param memoryId           UUID of the memory fragment
     * @param tenantId           tenant scope identifier
     * @param kind               memory kind
     * @param lifecycleState     lifecycle state
     * @param sensitivityLevel   sensitivity level
     * @param scopeId            scope identifier
     * @return payload map for Qdrant upsert
     */
    public static Map<String, Object> buildUpsertPayload(
            String memoryId,
            String tenantId,
            String kind,
            String lifecycleState,
            String sensitivityLevel,
            String scopeId
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("memory_id", memoryId);
        payload.put("tenant_id", tenantId);
        if (kind != null) payload.put("kind", kind);
        if (lifecycleState != null) payload.put("lifecycle_state", lifecycleState);
        if (sensitivityLevel != null) payload.put("sensitivity_level", sensitivityLevel);
        if (scopeId != null && !scopeId.isBlank()) payload.put("scope_id", scopeId);
        return payload;
    }

    /** Variant with namespace support. */
    public static Map<String, Object> buildUpsertPayload(
            String memoryId, String tenantId, String kind, String lifecycleState,
            String sensitivityLevel, String scopeId, String namespace) {
        Map<String, Object> payload = buildUpsertPayload(memoryId, tenantId, kind,
                lifecycleState, sensitivityLevel, scopeId);
        if (namespace != null && !namespace.isBlank()) payload.put("namespace", namespace);
        return payload;
    }
}

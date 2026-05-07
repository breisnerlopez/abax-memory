package com.abax.memory.domain.service;

import com.abax.memory.api.dto.v2.GraphResponse;
import com.abax.memory.api.dto.v2.MemoryResponse;
import com.abax.memory.api.dto.v2.SearchResponse;
import com.abax.memory.api.dto.v2.SemanticSearchRequest;
import com.abax.memory.api.dto.v2.UnifiedSearchRequest;
import com.abax.memory.api.dto.v2.UnifiedSearchResponse;
import com.abax.memory.domain.model.GraphStrategyOverride;

import java.util.List;
import java.util.UUID;

/**
 * Service contract for semantic search, hybrid search, similarity,
 * and graph traversal — v2.0.0 EP-005.
 *
 * <p>References: EP-005, HU-005.1.1 through HU-005.7.1,
 * Architecture document §5</p>
 */
public interface SearchService {

    /**
     * Performs a pure semantic (vector-only) search.
     *
     * @param request  query text and optional filters
     * @param tenantId tenant scope identifier
     * @return paginated search results with facets
     */
    SearchResponse semanticSearch(SemanticSearchRequest request, String tenantId);

    /**
     * Performs a hybrid search combining semantic (vector) and
     * keyword (full-text) relevance scores.
     *
     * @param request  query text and optional filters
     * @param tenantId tenant scope identifier
     * @return paginated search results with facets
     */
    SearchResponse hybridSearch(SemanticSearchRequest request, String tenantId);

    /**
     * Finds memory fragments semantically similar to the given fragment.
     *
     * @param fragmentId UUID of the source memory fragment
     * @param tenantId   tenant scope identifier
     * @param limit      maximum number of results (1–50)
     * @return list of similar memory responses, excluding the source itself
     */
    List<MemoryResponse> findSimilar(UUID fragmentId, String tenantId, int limit);

    /**
     * Expands the relationship graph around a central node up to
     * the given depth using BFS traversal.
     *
     * @param fragmentId UUID of the central node
     * @param depth      expansion depth (1–5)
     * @param tenantId   tenant scope identifier
     * @return graph response with center node, relations, and all nodes
     */
    GraphResponse expandGraph(UUID fragmentId, int depth, String tenantId);

    /**
     * Indexes a single memory fragment into the vector store (Qdrant).
     * Called after memory creation or update. If the vector store is
     * unavailable, logs a warning and does not block the operation.
     *
     * @param fragmentId UUID of the memory fragment to index
     * @param tenantId   tenant scope identifier
     */
    void indexFragment(UUID fragmentId, String tenantId);

    /**
     * Re-indexes all active (non-deleted) memory fragments for a tenant.
     * Admin-only operation. Used when the embedding model changes.
     *
     * @param tenantId tenant scope identifier
     * @return number of fragments indexed
     */
    int reindexAll(String tenantId);

    /**
     * Performs a unified search combining vector (semantic + keyword hybrid)
     * with graph expansion transparently.
     *
     * <p>The caller receives a single sorted result set — the consumer does
     * not need to know whether any individual result came from vectors or
     * graph traversal. The response metadata exposes the composition for
     * debugging/observability.</p>
     *
     * @param request  query text, filters, and graph-expansion controls
     * @param tenantId tenant scope identifier
     * @return unified search response with merged vector and graph results
     */
    UnifiedSearchResponse unifiedSearch(UnifiedSearchRequest request, String tenantId);

    /**
     * Performs a unified search with graph strategy override from HTTP headers — v2.1.0.
     *
     * @param request          query text, filters, and graph-expansion controls
     * @param tenantId         tenant scope identifier
     * @param strategyOverride override from {@code X-Graph-*} headers, or {@code null}
     * @return unified search response with merged vector and graph results
     */
    UnifiedSearchResponse unifiedSearch(UnifiedSearchRequest request, String tenantId,
                                         GraphStrategyOverride strategyOverride);
}

package com.abax.memory.infrastructure.service;

import com.abax.memory.api.dto.v2.GraphEdge;
import com.abax.memory.api.dto.v2.GraphResponse;
import com.abax.memory.api.dto.v2.MemoryResponse;
import com.abax.memory.api.dto.v2.ScoredMemory;
import com.abax.memory.api.dto.v2.SearchResponse;
import com.abax.memory.api.dto.v2.SemanticSearchRequest;
import com.abax.memory.api.dto.v2.UnifiedSearchRequest;
import com.abax.memory.api.dto.v2.UnifiedSearchResponse;
import com.abax.memory.domain.enums.LifecycleState;
import com.abax.memory.domain.enums.MemoryKind;
import com.abax.memory.domain.enums.SensitivityLevel;
import com.abax.memory.domain.model.Relation;
import com.abax.memory.domain.model.InferredRelation;
import com.abax.memory.domain.model.MemoryFragment;
import com.abax.memory.domain.service.LlmService;
import com.abax.memory.domain.service.RelationService;
import com.abax.memory.domain.service.SearchService;
import com.abax.memory.infrastructure.ai.EmbeddingProvider;
import com.abax.memory.infrastructure.persistence.MemoryFragmentEntity;
import com.abax.memory.infrastructure.qdrant.QdrantClient;
import com.abax.memory.infrastructure.security.TenantContext;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link SearchService} — EP-005.
 *
 * <h3>Semantic Search</h3>
 * Generates an embedding for the query text via {@link EmbeddingProvider},
 * searches the Qdrant vector store via {@link QdrantClient}, loads the
 * corresponding {@link MemoryFragmentEntity} records, applies additional
 * post-search filters, and returns a paginated {@link SearchResponse}.
 *
 * <h3>Hybrid Search</h3>
 * Combines vector similarity scores with full-text keyword matching for
 * improved recall. The in-memory mock uses a simple TF-IDF approximation.
 *
 * <h3>Graph Traversal</h3>
 * Performs BFS over the relations table starting from a central node,
 * expanding up to the specified depth.
 *
 * <p>References: EP-005, HU-005.1.1 through HU-005.7.1,
 * Architecture document §5</p>
 */
@ApplicationScoped
public class SearchServiceImpl implements SearchService {

    private static final Logger LOG = Logger.getLogger(SearchServiceImpl.class);

    // MOCK: Collection name hardcoded — Qdrant server not available.
    // REPLACE_BEFORE_PROD with configurable collection per tenant.
    private static final String QDRANT_COLLECTION = "abax-memories-v2";

    private static final int MAX_SIMILAR_LIMIT = 50;
    private static final int MAX_GRAPH_DEPTH = 5;

    @Inject
    QdrantClient qdrantClient;

    @Inject
    EmbeddingProvider embeddingProvider;

    @Inject
    RelationService relationService;

    @Inject
    LlmService llmService;

    @Inject
    TenantContext tenantContext;

    @ConfigProperty(name = "abax.v2.search.default-topk", defaultValue = "10")
    int defaultTopK;

    // ── Semantic Search ──────────────────────────────────────────────

    @Override
    public SearchResponse semanticSearch(SemanticSearchRequest request, String tenantId) {
        int topK = clampTopK(request.getTopK());
        int page = Math.max(0, request.getPage());
        int size = Math.max(1, Math.min(100, request.getSize()));

        // 1. Generate embedding for query
        float[] queryVector = embeddingProvider.embed(request.getQuery());

        // 2. Build Qdrant filters
        Map<String, Object> qdrantFilters = SearchFilterBuilder.buildQdrantFilters(request, tenantId);

        // 3. Search Qdrant
        List<QdrantClient.ScoredHit> hits = qdrantClient.search(
                QDRANT_COLLECTION, queryVector, qdrantFilters, topK);

        // 4. Load MemoryFragment entities for matched point IDs
        List<String> pointIds = hits.stream()
                .map(QdrantClient.ScoredHit::pointId)
                .toList();
        Map<String, MemoryFragmentEntity> entityMap = loadEntitiesByIds(pointIds, tenantId);

        // 5. Build results with post-search filtering
        List<MemoryResponse> allResults = new ArrayList<>();
        for (QdrantClient.ScoredHit hit : hits) {
            MemoryFragmentEntity entity = entityMap.get(hit.pointId());
            if (entity != null && passesPostFilter(entity, request)) {
                allResults.add(MemoryResponse.from(entity));
            }
        }

        // 6. Paginate
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, allResults.size());
        List<MemoryResponse> pageItems = fromIndex < allResults.size()
                ? allResults.subList(fromIndex, toIndex)
                : List.of();

        // 7. Build facets from results (not from full DB)
        Map<String, Map<String, Long>> facets = buildResultFacets(allResults);

        return new SearchResponse(pageItems, allResults.size(), page, size, facets);
    }

    // ── Hybrid Search ────────────────────────────────────────────────

    @Override
    public SearchResponse hybridSearch(SemanticSearchRequest request, String tenantId) {
        int topK = clampTopK(request.getTopK());
        int page = Math.max(0, request.getPage());
        int size = Math.max(1, Math.min(100, request.getSize()));

        // 1. Semantic search (wider retrieval)
        float[] queryVector = embeddingProvider.embed(request.getQuery());
        Map<String, Object> qdrantFilters = SearchFilterBuilder.buildQdrantFilters(request, tenantId);

        // Retrieve 2x topK for hybrid re-ranking
        List<QdrantClient.ScoredHit> semanticHits = qdrantClient.search(
                QDRANT_COLLECTION, queryVector, qdrantFilters, Math.max(topK * 2, 20));

        // 2. Load entities
        List<String> pointIds = semanticHits.stream()
                .map(QdrantClient.ScoredHit::pointId)
                .toList();
        Map<String, MemoryFragmentEntity> entityMap = loadEntitiesByIds(pointIds, tenantId);

        // 3. Hybrid scoring: 0.7 * semantic + 0.3 * keyword
        String queryLower = request.getQuery().toLowerCase();
        List<ScoredResult> scored = new ArrayList<>();
        for (QdrantClient.ScoredHit hit : semanticHits) {
            MemoryFragmentEntity entity = entityMap.get(hit.pointId());
            if (entity != null && passesPostFilter(entity, request)) {
                double keywordScore = computeKeywordScore(queryLower, entity);
                double hybridScore = 0.7 * hit.score() + 0.3 * keywordScore;
                scored.add(new ScoredResult(entity, hybridScore));
            }
        }

        // 4. Sort by hybrid score descending
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        // 5. Limit to topK after re-ranking
        List<MemoryResponse> allResults = scored.stream()
                .limit(topK)
                .map(sr -> MemoryResponse.from(sr.entity))
                .toList();

        // 6. Paginate
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, allResults.size());
        List<MemoryResponse> pageItems = fromIndex < allResults.size()
                ? allResults.subList(fromIndex, toIndex)
                : List.of();

        // 7. Build facets
        Map<String, Map<String, Long>> facets = buildResultFacets(allResults);

        return new SearchResponse(pageItems, allResults.size(), page, size, facets);
    }

    // ── Unified Search ───────────────────────────────────────────────

    @Override
    public UnifiedSearchResponse unifiedSearch(UnifiedSearchRequest request, String tenantId) {
        int page = Math.max(0, request.getPage());
        int size = Math.max(1, Math.min(100, request.getSize()));

        // Step 1: hybrid search (vector + keyword) — retrieve 2x size for richer merge pool
        SemanticSearchRequest semiRequest = new SemanticSearchRequest(
                request.getQuery(),
                request.getKinds(),
                request.getLifecycleStates(),
                request.getSensitivityMax(),
                request.getScopeIds(),
                null, // fromDate — not used in unified search v1
                null, // toDate
                0,    // page 0 — we paginate after merge
                Math.max(size * 2, 40), // wider retrieval
                10    // topK not relevant at this stage
        );
        SearchResponse vectorResults = hybridSearch(semiRequest, tenantId);

        Set<UUID> seenIds = new HashSet<>();
        List<ScoredMemory> unified = new ArrayList<>();
        int graphContributions = 0;

        // Step 2: add vector (hybrid) results — assign score from hybrid ranking
        // Note: hybridSearch already sorted results by hybrid score; we preserve the
        // original order for relative ranking but normalize scores for graph blending.
        for (int i = 0; i < vectorResults.items().size(); i++) {
            MemoryResponse item = vectorResults.items().get(i);
            seenIds.add(item.id());
            // Normalize positional score: top result gets ~1.0, decaying linearly
            double normalizedScore = 1.0 - ((double) i / Math.max(vectorResults.items().size(), 1)) * 0.3;
            MemoryResponse scoredItem = withScore(item, normalizedScore);
            unified.add(new ScoredMemory(scoredItem, "vector"));
        }

        // Step 3: expand graph from top-K results
        if (request.isExpandGraph() && !vectorResults.items().isEmpty()) {
            int topK = Math.max(1, Math.min(request.getGraphTopK(), vectorResults.items().size()));
            List<MemoryResponse> topKResults = vectorResults.items().subList(0, topK);

            for (MemoryResponse entry : topKResults) {
                try {
                    GraphResponse graph = expandGraph(entry.id(), request.getGraphDepth(), tenantId);
                    if (graph.nodes() != null) {
                        for (MemoryResponse node : graph.nodes()) {
                            if (!seenIds.contains(node.id())) {
                                seenIds.add(node.id());
                                // Graph score: 70% of the connecting seed's score
                                double seedScore = entry.score() != null ? entry.score() : 0.7;
                                double graphScore = seedScore * 0.7;
                                MemoryResponse scoredNode = withScore(node, graphScore);
                                unified.add(new ScoredMemory(scoredNode, "graph"));
                                graphContributions++;
                            }
                        }
                    }
                } catch (Exception e) {
                    LOG.debugv("Graph expansion failed for seed {0}: {1}",
                            entry.id(), e.getMessage());
                    // Non-blocking: graph expansion errors don't fail the search
                }
            }
        }

        // Step 4: sort by score descending
        unified.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        // Step 5: paginate
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, unified.size());
        List<ScoredMemory> pageItems = fromIndex < unified.size()
                ? unified.subList(fromIndex, toIndex)
                : List.of();

        // Step 6: build facets from the full unified result set
        List<MemoryResponse> allResponses = unified.stream()
                .map(ScoredMemory::getMemory)
                .toList();
        Map<String, Map<String, Long>> facets = buildResultFacets(allResponses);

        return new UnifiedSearchResponse(
                pageItems, unified.size(), page, size,
                request.isExpandGraph(), graphContributions, facets
        );
    }

    // ── Find Similar ─────────────────────────────────────────────────

    @Override
    public List<MemoryResponse> findSimilar(UUID fragmentId, String tenantId, int limit) {
        int effectiveLimit = Math.max(1, Math.min(MAX_SIMILAR_LIMIT, limit));

        // 1. Load source fragment
        MemoryFragmentEntity source = requireEntityForTenant(fragmentId, tenantId);

        // 2. Generate embedding for source content (on-the-fly)
        float[] sourceVector = embeddingProvider.embed(source.getContent());

        // 3. Search Qdrant — exclude source itself, retrieve extra for filtering
        Map<String, Object> filters = Map.of("tenant_id", tenantId);
        List<QdrantClient.ScoredHit> hits = qdrantClient.search(
                QDRANT_COLLECTION, sourceVector, filters, effectiveLimit + 1);

        // 4. Map to responses, exclude source
        return hits.stream()
                .filter(hit -> !hit.pointId().equals(fragmentId.toString()))
                .limit(effectiveLimit)
                .map(hit -> MemoryFragmentEntity.<MemoryFragmentEntity>findById(UUID.fromString(hit.pointId())))
                .filter(entity -> entity != null && !entity.isDeleted())
                .map(MemoryResponse::from)
                .toList();
    }

    // ── Graph Expansion ──────────────────────────────────────────────

    @Override
    public GraphResponse expandGraph(UUID fragmentId, int depth, String tenantId) {
        int effectiveDepth = Math.max(1, Math.min(MAX_GRAPH_DEPTH, depth > 0 ? depth : 2));

        // Load center node
        MemoryFragmentEntity center = requireEntityForTenant(fragmentId, tenantId);
        MemoryResponse centerResponse = MemoryResponse.from(center);

        // BFS traversal
        Set<UUID> visited = new HashSet<>();
        List<GraphEdge> edges = new ArrayList<>();
        Map<UUID, MemoryFragmentEntity> nodeMap = new LinkedHashMap<>();
        nodeMap.put(center.getId(), center);

        Deque<UUID> queue = new ArrayDeque<>();
        queue.add(center.getId());
        visited.add(center.getId());

        for (int level = 0; level < effectiveDepth && !queue.isEmpty(); level++) {
            int levelSize = queue.size();
            for (int i = 0; i < levelSize; i++) {
                UUID currentId = queue.poll();
                if (currentId == null) continue;

                // Get outgoing relations (source = currentId)
                List<Relation> outgoing = relationService.findBySource(currentId);
                for (Relation rel : outgoing) {
                    addEdgeAndEnqueue(rel, rel.getSourceId(), rel.getTargetId(),
                            visited, queue, edges, nodeMap, tenantId);
                }

                // Get incoming relations and follow reverse if bidirectional
                List<Relation> incoming = relationService.findByTarget(currentId);
                for (Relation rel : incoming) {
                    if (rel.getType().isTraversableReverse()) {
                        addEdgeAndEnqueue(rel, rel.getSourceId(), rel.getTargetId(),
                                visited, queue, edges, nodeMap, tenantId);
                    } else {
                        // Still include the edge but don't traverse
                        edges.add(new GraphEdge(rel.getSourceId(), rel.getTargetId(),
                                rel.getType().name(), Map.of()));
                        // Load node if not already loaded
                        if (!nodeMap.containsKey(rel.getSourceId())) {
                            loadIntoNodeMap(rel.getSourceId(), nodeMap, tenantId);
                        }
                    }
                }
            }
        }

        List<MemoryResponse> nodes = nodeMap.values().stream()
                .map(MemoryResponse::from)
                .toList();

        // D4: LLM-inferred relations — suggest additional edges between
        // the center node and other nodes in the graph that the LLM detects.
        try {
            var centerDomain = toDomainFragment(center);
            var candidates = nodeMap.values().stream()
                    .filter(e -> !e.getId().equals(center.getId()))
                    .map(this::toDomainFragment)
                    .collect(Collectors.toList());
            if (!candidates.isEmpty()) {
                List<InferredRelation> inferred = llmService.inferRelations(centerDomain, candidates);
                for (InferredRelation ir : inferred) {
                    // Add inferred edges that don't duplicate existing ones
                    boolean isDuplicate = edges.stream().anyMatch(e ->
                            e.sourceId().equals(ir.sourceId())
                                    && e.targetId().equals(ir.targetId())
                                    && e.relationType().equalsIgnoreCase(ir.relationType().name()));
                    if (!isDuplicate) {
                        edges.add(new GraphEdge(ir.sourceId(), ir.targetId(),
                                ir.relationType().name(),
                                Map.of("inferred", true, "confidence", ir.confidence(),
                                        "evidence", ir.evidence())));
                    }
                }
            }
        } catch (Exception e) {
            LOG.debugv("LLM relation inference skipped for graph expansion: {0}", e.getMessage());
            // Non-blocking: graph expansion succeeds even without LLM enrichment
        }

        return new GraphResponse(centerResponse, edges, nodes);
    }

    private void addEdgeAndEnqueue(Relation rel, UUID sourceId, UUID targetId,
                                    Set<UUID> visited, Deque<UUID> queue,
                                    List<GraphEdge> edges,
                                    Map<UUID, MemoryFragmentEntity> nodeMap,
                                    String tenantId) {
        edges.add(new GraphEdge(sourceId, targetId, rel.getType().name(), Map.of()));
        // Load target node if not visited
        if (visited.add(targetId)) {
            queue.add(targetId);
            loadIntoNodeMap(targetId, nodeMap, tenantId);
        }
        // Ensure source node is loaded
        if (!nodeMap.containsKey(sourceId)) {
            loadIntoNodeMap(sourceId, nodeMap, tenantId);
        }
    }

    private void loadIntoNodeMap(UUID nodeId, Map<UUID, MemoryFragmentEntity> nodeMap, String tenantId) {
        if (nodeMap.containsKey(nodeId)) return;
        MemoryFragmentEntity entity = MemoryFragmentEntity.findById(nodeId);
        if (entity != null && !entity.isDeleted()
                && tenantId.equals(entity.getTenantId())) {
            nodeMap.put(nodeId, entity);
        }
    }

    // ── Index Fragment ───────────────────────────────────────────────

    @Override
    public void indexFragment(UUID fragmentId, String tenantId) {
        MemoryFragmentEntity entity = MemoryFragmentEntity.findById(fragmentId);
        if (entity == null || entity.isDeleted()) {
            LOG.warnv("Cannot index fragment {0}: not found or deleted", fragmentId);
            return;
        }
        if (!tenantId.equals(entity.getTenantId())) {
            LOG.warnv("Cannot index fragment {0}: cross-tenant access", fragmentId);
            return;
        }

        try {
            String text = buildIndexableText(entity);
            float[] vector = embeddingProvider.embed(text);
            Map<String, Object> payload = SearchFilterBuilder.buildUpsertPayload(
                    entity.getId().toString(),
                    entity.getTenantId(),
                    entity.getKind() != null ? entity.getKind().name() : null,
                    entity.getLifecycleState() != null ? entity.getLifecycleState().name() : null,
                    entity.getSensitivityLevel() != null ? entity.getSensitivityLevel().name() : null,
                    entity.getScopeId()
            );

            qdrantClient.upsert(QDRANT_COLLECTION, entity.getId().toString(), vector, payload);
            // Issue #17: write embedding_id back to PostgreSQL so semantic search
            // can return non-null scores for newly indexed memories.
            entity.setEmbeddingId(entity.getId().toString());
            entity.persist();
            LOG.debugv("Indexed fragment {0} for tenant {1}", fragmentId, tenantId);
        } catch (Exception e) {
            // MOCK: Qdrant is in-memory and won't fail in test,
            // but in production this could be a network exception.
            // REPLACE_BEFORE_PROD: add retry with exponential backoff.
            LOG.warnv("Failed to index fragment {0}: {1}", fragmentId, e.getMessage());
        }
    }

    // ── Re-index All ─────────────────────────────────────────────────

    @Override
    @Transactional
    public int reindexAll(String tenantId) {
        List<MemoryFragmentEntity> entities = MemoryFragmentEntity.find(
                "tenantId = :tenantId and deletedAt IS NULL",
                Map.of("tenantId", tenantId))
                .list();

        int indexed = 0;
        for (MemoryFragmentEntity entity : entities) {
            try {
                String text = buildIndexableText((MemoryFragmentEntity) entity);
                float[] vector = embeddingProvider.embed(text);
                Map<String, Object> payload = SearchFilterBuilder.buildUpsertPayload(
                        entity.getId().toString(),
                        entity.getTenantId(),
                        entity.getKind() != null ? entity.getKind().name() : null,
                        entity.getLifecycleState() != null ? entity.getLifecycleState().name() : null,
                        entity.getSensitivityLevel() != null ? entity.getSensitivityLevel().name() : null,
                        entity.getScopeId()
                );
                qdrantClient.upsert(QDRANT_COLLECTION, entity.getId().toString(), vector, payload);
                // Issue #17: write embedding_id back to PostgreSQL.
                ((MemoryFragmentEntity) entity).setEmbeddingId(entity.getId().toString());
                ((MemoryFragmentEntity) entity).persist();
                indexed++;
            } catch (Exception e) {
                LOG.warnv("Failed to re-index fragment {0}: {1}",
                        ((MemoryFragmentEntity) entity).getId(), e.getMessage());
            }
        }

        LOG.infov("Re-indexed {0}/{1} fragments for tenant {2}", indexed, entities.size(), tenantId);
        return indexed;
    }

    // ── Private Helpers ──────────────────────────────────────────────

    /**
     * Loads memory fragment entities by their IDs, filtered by tenant.
     */
    private Map<String, MemoryFragmentEntity> loadEntitiesByIds(List<String> ids, String tenantId) {
        if (ids.isEmpty()) return Map.of();
        Map<String, MemoryFragmentEntity> result = new LinkedHashMap<>();
        for (String idStr : ids) {
            try {
                UUID uuid = UUID.fromString(idStr);
                MemoryFragmentEntity entity = MemoryFragmentEntity.findById(uuid);
                if (entity != null && !entity.isDeleted()
                        && tenantId.equals(entity.getTenantId())) {
                    result.put(idStr, entity);
                }
            } catch (IllegalArgumentException e) {
                LOG.debugv("Invalid UUID in Qdrant point ID: {0}", idStr);
            }
        }
        return result;
    }

    /**
     * Applies post-search filters that Qdrant cannot handle natively.
     */
    private boolean passesPostFilter(MemoryFragmentEntity entity, SemanticSearchRequest request) {
        // Lifecycle state filter
        if (request.getLifecycleStates() != null && !request.getLifecycleStates().isEmpty()) {
            if (!request.getLifecycleStates().contains(entity.getLifecycleState())) {
                return false;
            }
        }

        // Sensitivity max filter (ordinal-based)
        if (request.getSensitivityMax() != null) {
            if (entity.getSensitivityLevel().ordinal() > request.getSensitivityMax().ordinal()) {
                return false;
            }
        }

        // Scope filter
        if (request.getScopeIds() != null && !request.getScopeIds().isEmpty()) {
            if (entity.getScopeId() == null
                    || !request.getScopeIds().contains(entity.getScopeId())) {
                return false;
            }
        }

        // Date range filters
        if (request.getFromDate() != null) {
            if (entity.getCreatedAt().isBefore(request.getFromDate())) {
                return false;
            }
        }
        if (request.getToDate() != null) {
            if (entity.getCreatedAt().isAfter(request.getToDate())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Computes a simple keyword relevance score.
     * In production, this would be replaced by a proper full-text search
     * engine (e.g., PostgreSQL tsvector or Elasticsearch).
     */
    // MOCK: Simple keyword frequency scoring — no full-text index available.
    // REPLACE_BEFORE_PROD: use PostgreSQL ts_rank or Elasticsearch BM25.
    private double computeKeywordScore(String queryLower, MemoryFragmentEntity entity) {
        String titleLower = entity.getTitle() != null ? entity.getTitle().toLowerCase() : "";
        String contentLower = entity.getContent() != null ? entity.getContent().toLowerCase() : "";
        String summaryLower = entity.getSummary() != null ? entity.getSummary().toLowerCase() : "";

        // Split query into terms
        String[] terms = queryLower.split("\\s+");
        double score = 0.0;

        for (String term : terms) {
            if (term.isBlank()) continue;
            // Title matches are weighted 3x
            score += 3.0 * countOccurrences(titleLower, term);
            // Content matches are weighted 1x
            score += 1.0 * countOccurrences(contentLower, term);
            // Summary matches are weighted 2x
            score += 2.0 * countOccurrences(summaryLower, term);
        }

        // Normalize by total text length (approximate BM25-ish normalization)
        double totalLength = titleLower.length() + contentLower.length() + summaryLower.length();
        if (totalLength == 0) return 0.0;
        return score / Math.sqrt(totalLength + 1);
    }

    private long countOccurrences(String text, String term) {
        long count = 0;
        int idx = 0;
        while ((idx = text.indexOf(term, idx)) != -1) {
            count++;
            idx += term.length();
        }
        return count;
    }

    /**
     * Builds the text to embed: title + summary + content.
     */
    private String buildIndexableText(MemoryFragmentEntity entity) {
        StringBuilder sb = new StringBuilder();
        if (entity.getTitle() != null) sb.append(entity.getTitle()).append(". ");
        if (entity.getSummary() != null) sb.append(entity.getSummary()).append(". ");
        if (entity.getContent() != null) {
            // Truncate very long content to 8000 chars for embedding
            String content = entity.getContent();
            if (content.length() > 8000) {
                content = content.substring(0, 8000);
            }
            sb.append(content);
        }
        return sb.toString();
    }

    /**
     * Builds facets from the result set (not from the full database).
     */
    private Map<String, Map<String, Long>> buildResultFacets(List<MemoryResponse> results) {
        Map<String, Map<String, Long>> facets = new LinkedHashMap<>();

        // Kind facet
        Map<String, Long> kindCounts = new LinkedHashMap<>();
        for (MemoryResponse r : results) {
            kindCounts.merge(r.kind().name(), 1L, Long::sum);
        }
        facets.put("kind", kindCounts);

        // Lifecycle state facet
        Map<String, Long> stateCounts = new LinkedHashMap<>();
        for (MemoryResponse r : results) {
            stateCounts.merge(r.lifecycleState().name(), 1L, Long::sum);
        }
        facets.put("lifecycleState", stateCounts);

        // Sensitivity level facet
        Map<String, Long> sensitivityCounts = new LinkedHashMap<>();
        for (MemoryResponse r : results) {
            sensitivityCounts.merge(r.sensitivityLevel().name(), 1L, Long::sum);
        }
        facets.put("sensitivityLevel", sensitivityCounts);

        return facets;
    }

    private int clampTopK(int topK) {
        return Math.max(1, Math.min(50, topK > 0 ? topK : defaultTopK));
    }

    /**
     * Loads and validates a MemoryFragmentEntity by ID with tenant isolation.
     */
    private MemoryFragmentEntity requireEntityForTenant(UUID id, String tenantId) {
        MemoryFragmentEntity entity = MemoryFragmentEntity.findById(id);
        if (entity == null || entity.isDeleted()) {
            throw new NotFoundException("Memory fragment not found: " + id);
        }
        if (!tenantId.equals(entity.getTenantId())) {
            LOG.warnv("Cross-tenant access attempt: id={0}, requesting_tenant={1}, resource_tenant={2}",
                    id, tenantId, entity.getTenantId());
            throw new NotFoundException("Memory fragment not found: " + id);
        }
        return entity;
    }

    /**
     * Internal holder for hybrid search scoring.
     */
    private record ScoredResult(MemoryFragmentEntity entity, double score) {
    }

    /**
     * Creates a copy of a {@link MemoryResponse} with the given score.
     */
    private static MemoryResponse withScore(MemoryResponse original, double score) {
        return new MemoryResponse(
                original.id(), original.tenantId(), original.scopeId(),
                original.namespace(), original.kind(),
                original.title(), original.content(), original.summary(),
                original.lifecycleState(), original.sensitivityLevel(),
                original.sourceType(), original.sourceRef(),
                original.confidence(), original.embeddingId(),
                original.reviewerId(), original.reviewComment(),
                original.createdAt(), original.updatedAt(), original.deletedAt(),
                original.isDeleted(), original.isConsumerVisible(),
                score
        );
    }

    /**
     * Converts a JPA entity to a domain {@link MemoryFragment} for LLM consumption.
     */
    private MemoryFragment toDomainFragment(MemoryFragmentEntity entity) {
        var fragment = new MemoryFragment();
        fragment.setId(entity.getId());
        fragment.setTenantId(entity.getTenantId());
        fragment.setKind(entity.getKind());
        fragment.setTitle(entity.getTitle());
        fragment.setContent(entity.getContent());
        fragment.setSummary(entity.getSummary());
        fragment.setLifecycleState(entity.getLifecycleState());
        fragment.setSensitivityLevel(entity.getSensitivityLevel());
        return fragment;
    }
}

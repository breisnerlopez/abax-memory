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
import com.abax.memory.domain.model.InferredRelation;
import com.abax.memory.domain.model.MemoryFragment;
import com.abax.memory.domain.model.RerankedHit;
import com.abax.memory.domain.service.CrossEncoderService;
import com.abax.memory.domain.service.LlmService;
import com.abax.memory.domain.service.RelationService;
import com.abax.memory.domain.service.SearchService;
import com.abax.memory.infrastructure.ai.EmbeddingProvider;
import com.abax.memory.infrastructure.persistence.MemoryFragmentEntity;
import com.abax.memory.infrastructure.qdrant.QdrantClient;
import com.abax.memory.infrastructure.security.TenantContext;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

    @Inject
    CrossEncoderService crossEncoderService;

    @ConfigProperty(name = "abax.v2.search.default-topk", defaultValue = "10")
    int defaultTopK;

    @ConfigProperty(name = "abax.v2.reranker.enabled", defaultValue = "true")
    boolean rerankerEnabled;

    @ConfigProperty(name = "abax.v2.qdrant.collection", defaultValue = "abax-memories")
    String qdrantCollection;

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
                qdrantCollection, queryVector, qdrantFilters, topK);

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
                // Issue #18: propagate Qdrant relevance score to API response
                allResults.add(withScore(MemoryResponse.from(entity), (double) hit.score()));
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
                qdrantCollection, queryVector, qdrantFilters, Math.max(topK * 2, 20));

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
                .map(sr -> withScore(MemoryResponse.from(sr.entity), sr.score))
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

    // ── Unified Search (v2.1.0 two-stage pipeline) ──────────────────

    @Override
    public UnifiedSearchResponse unifiedSearch(UnifiedSearchRequest request, String tenantId) {
        long startTime = System.currentTimeMillis();
        int page = Math.max(0, request.getPage());
        int size = Math.max(1, Math.min(100, request.getSize()));
        int denseTopK = Math.max(size, 20); // retrieve at least 20 for reranker

        // ── Stage 1: Dense retrieval (pure semantic, no keyword mixing) ──
        SemanticSearchRequest semiRequest = new SemanticSearchRequest(
                request.getQuery(),
                request.getKinds(),
                request.getLifecycleStates(),
                request.getSensitivityMax(),
                request.getScopeIds(),
                null, null,
                0, denseTopK, 10
        );
        SearchResponse denseResults = semanticSearch(semiRequest, tenantId);

        int denseCandidates = denseResults.items().size();
        List<String> pipelineStages = new ArrayList<>();
        pipelineStages.add("dense-retrieval");
        boolean crossEncoderApplied = false;

        // ── Stage 2: Cross-encoder reranker (optional, graceful degradation) ──
        List<MemoryResponse> rankedResults = denseResults.items();
        Map<String, Double> crossScores = new HashMap<>();
        if (request.isRerank() && rerankerEnabled && !denseResults.items().isEmpty()) {
            try {
                // Build candidate documents for the cross-encoder
                List<CrossEncoderService.CandidateDocument> candidates = new ArrayList<>();
                for (MemoryResponse item : denseResults.items()) {
                    String content = extractContentForReranking(item);
                    candidates.add(new CrossEncoderService.CandidateDocument(
                            item.id().toString(), content,
                            item.score() != null ? item.score() : 0.5));
                }

                List<RerankedHit> reranked = crossEncoderService.rerank(
                        request.getQuery(), candidates, Math.min(size, 20));

                if (!reranked.isEmpty()) {
                    crossEncoderApplied = true;
                    pipelineStages.add("cross-encoder-reranker");

                    // Rebuild ranked results in cross-encoder order
                    Map<String, MemoryResponse> itemMap = new HashMap<>();
                    for (MemoryResponse item : denseResults.items()) {
                        itemMap.put(item.id().toString(), item);
                    }

                    List<MemoryResponse> reordered = new ArrayList<>();
                    for (RerankedHit hit : reranked) {
                        MemoryResponse item = itemMap.get(hit.memoryId());
                        if (item != null) {
                            reordered.add(withScore(item, hit.finalScore()));
                            crossScores.put(hit.memoryId(), hit.crossEncoderScore());
                        }
                    }
                    // Append any candidates not scored by reranker at the bottom
                    Set<String> scored = reranked.stream().map(RerankedHit::memoryId).collect(Collectors.toSet());
                    for (MemoryResponse item : denseResults.items()) {
                        if (!scored.contains(item.id().toString()) && reordered.size() < denseTopK) {
                            reordered.add(item);
                        }
                    }
                    rankedResults = reordered;
                } else {
                    LOG.debug("Cross-encoder returned empty — using dense-only ordering");
                }
            } catch (Exception e) {
                LOG.warnv(e, "Cross-encoder unavailable for this query — using dense-only ordering");
                // Graceful degradation: continue with dense-only results
            }
        }

        // ── Build semantic results (before graph) ──
        Set<UUID> seenIds = new HashSet<>();
        List<ScoredMemory> unified = new ArrayList<>();

        for (MemoryResponse item : rankedResults) {
            if (seenIds.add(item.id())) {
                Map<String, Double> scoreComponents = new LinkedHashMap<>();
                scoreComponents.put("semantic", item.score() != null ? item.score() : 0.0);
                if (crossScores.containsKey(item.id().toString())) {
                    scoreComponents.put("crossEncoder", crossScores.get(item.id().toString()));
                }
                String pipeLabel = crossEncoderApplied ? "two-stage" : "dense-only";
                unified.add(new ScoredMemory(item, "vector", scoreComponents, pipeLabel, false));
            }
        }

        // ── Graph expansion (only when expandGraph=true, isolated from semantic) ──
        boolean graphExpanded = false;
        int graphContributions = 0;
        int totalExpandedNodes = 0;
        int maxDepth = 0;
        List<String> entryPointIds = List.of();
        String entryPointSource = null;
        int entryPointCount = 0;

        if (request.isExpandGraph() && !rankedResults.isEmpty()) {
            graphExpanded = true;
            pipelineStages.add("graph-expansion");

            // Resolve entry points: explicit > semantic top-K
            Set<UUID> seeds;
            if (request.getEntryPoints() != null && !request.getEntryPoints().isEmpty()) {
                // Client-provided explicit entry points
                seeds = new LinkedHashSet<>();
                for (String ep : request.getEntryPoints()) {
                    try {
                        seeds.add(UUID.fromString(ep));
                    } catch (IllegalArgumentException e) {
                        LOG.warnv("ENTRY_POINT_NOT_FOUND: invalid UUID {0}", ep);
                    }
                }
                // Validate existence
                Map<UUID, MemoryFragmentEntity> validSeeds = loadEntitiesBatch(seeds, tenantId);
                seeds.retainAll(validSeeds.keySet());
                entryPointSource = "client-provided";
                entryPointCount = seeds.size();
            } else {
                // Auto-select from semantic top-K
                int topK = Math.max(1, Math.min(request.getGraphTopK(), rankedResults.size()));
                seeds = rankedResults.subList(0, topK).stream()
                        .map(MemoryResponse::id)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                entryPointSource = "dense-retrieval-top-" + topK;
                entryPointCount = topK;
            }

            entryPointIds = seeds.stream().map(UUID::toString).toList();

            if (!seeds.isEmpty()) {
                Map<UUID, Double> seedScores = rankedResults.stream()
                        .filter(r -> seeds.contains(r.id()))
                        .collect(Collectors.toMap(MemoryResponse::id,
                                r -> r.score() != null ? r.score() : 0.7));

                try {
                    GraphExpansionResult expanded = expandGraphConsolidated(
                            seeds, seedScores, request.getGraphDepth(), tenantId);

                    for (var entry : expanded.nodes().entrySet()) {
                        UUID nodeId = entry.getKey();
                        if (seenIds.add(nodeId)) {
                            double graphScore = entry.getValue();
                            MemoryResponse scoredNode = withScore(
                                    MemoryResponse.from(expanded.entityMap().get(nodeId)), graphScore);
                            Map<String, Double> graphComponents = new LinkedHashMap<>();
                            graphComponents.put("graph", graphScore);
                            unified.add(new ScoredMemory(scoredNode, "graph", graphComponents,
                                    "graph-expansion", true));
                            graphContributions++;
                        }
                    }
                    totalExpandedNodes = expanded.nodes().size();
                    maxDepth = request.getGraphDepth();
                } catch (Exception e) {
                    LOG.debugv("Graph expansion failed for unified search: {0}", e.getMessage());
                }
            }
        }

        // ── Sort by score descending ──
        unified.sort((a, b) -> Double.compare(
                b.getScore() != null ? b.getScore() : 0.0,
                a.getScore() != null ? a.getScore() : 0.0));

        // ── Paginate ──
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, unified.size());
        List<ScoredMemory> pageItems = fromIndex < unified.size()
                ? unified.subList(fromIndex, toIndex)
                : List.of();

        // ── Build facets ──
        List<MemoryResponse> allResponses = unified.stream()
                .map(ScoredMemory::getMemory)
                .toList();
        Map<String, Map<String, Long>> facets = buildResultFacets(allResponses);

        // ── Build pipeline metadata ──
        UnifiedSearchResponse.PipelineMetadata pipelineMeta = new UnifiedSearchResponse.PipelineMetadata(
                pipelineStages, crossEncoderApplied, denseCandidates, graphExpanded,
                graphExpanded ? new UnifiedSearchResponse.GraphExpandedNodes(
                        entryPointIds, entryPointCount, entryPointSource,
                        totalExpandedNodes, maxDepth, false) : null
        );

        long queryTimeMs = System.currentTimeMillis() - startTime;

        return new UnifiedSearchResponse(
                pageItems, unified.size(), page, size,
                graphExpanded, graphContributions, facets,
                queryTimeMs, pipelineMeta
        );
    }

    /**
     * Extracts content text from a MemoryResponse for cross-encoder evaluation.
     * Combines title, summary, and content (truncated to 1500 chars).
     */
    private String extractContentForReranking(MemoryResponse item) {
        StringBuilder sb = new StringBuilder();
        if (item.title() != null && !item.title().isBlank()) {
            sb.append(item.title()).append(". ");
        }
        if (item.summary() != null && !item.summary().isBlank()) {
            sb.append(item.summary()).append(". ");
        }
        if (item.content() != null) {
            String content = item.content();
            if (content.length() > 1500) {
                content = content.substring(0, 1497) + "...";
            }
            sb.append(content);
        }
        return sb.toString();
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
                qdrantCollection, sourceVector, filters, effectiveLimit + 1);

        // 4. Batch-load all matched entities (Fix 2: single query instead of N+1)
        List<String> hitPointIds = hits.stream()
                .filter(hit -> !hit.pointId().equals(fragmentId.toString()))
                .map(QdrantClient.ScoredHit::pointId)
                .toList();
        Map<String, MemoryFragmentEntity> entityMap = loadEntitiesByIds(hitPointIds, tenantId);

        // 5. Build results preserving Qdrant score order — Issue #18
        return hits.stream()
                .filter(hit -> !hit.pointId().equals(fragmentId.toString()))
                .map(hit -> {
                    MemoryFragmentEntity entity = entityMap.get(hit.pointId());
                    if (entity != null) {
                        return withScore(MemoryResponse.from(entity), (double) hit.score());
                    }
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .limit(effectiveLimit)
                .toList();
    }

    // ── Graph Expansion ──────────────────────────────────────────────

    @Override
    public GraphResponse expandGraph(UUID fragmentId, int depth, String tenantId) {
        int effectiveDepth = Math.max(1, Math.min(MAX_GRAPH_DEPTH, depth > 0 ? depth : 2));

        // Load center node
        MemoryFragmentEntity center = requireEntityForTenant(fragmentId, tenantId);
        MemoryResponse centerResponse = MemoryResponse.from(center);

        // BFS traversal — batch-optimized: one query per BFS level instead of N+1 per node
        Set<UUID> visited = new LinkedHashSet<>();  // preserves insertion order for output
        List<GraphEdge> edges = new ArrayList<>();
        Set<String> edgeKeys = new HashSet<>();  // deduplication: "sourceId|targetId|type"
        Map<UUID, MemoryFragmentEntity> nodeMap = new LinkedHashMap<>();
        nodeMap.put(center.getId(), center);

        Deque<UUID> queue = new ArrayDeque<>();
        queue.add(center.getId());
        visited.add(center.getId());

        for (int level = 0; level < effectiveDepth && !queue.isEmpty(); level++) {
            // Collect all node IDs in this BFS level
            Set<UUID> levelNodeIds = new LinkedHashSet<>(queue);

            // Fix 5: Batch-fetch ALL relations for this level in ONE query
            // instead of N*2 queries (findBySource + findByTarget per node)
            List<com.abax.memory.infrastructure.persistence.RelationEntity> allLevelRelations =
                    findRelationsForNodeIds(levelNodeIds, tenantId);

            // Index relations by source and target for O(1) lookup per node
            Map<UUID, List<com.abax.memory.infrastructure.persistence.RelationEntity>> bySource = new HashMap<>();
            Map<UUID, List<com.abax.memory.infrastructure.persistence.RelationEntity>> byTarget = new HashMap<>();
            for (var rel : allLevelRelations) {
                bySource.computeIfAbsent(rel.getSourceId(), k -> new ArrayList<>()).add(rel);
                byTarget.computeIfAbsent(rel.getTargetId(), k -> new ArrayList<>()).add(rel);
            }

            // Discover new neighbor IDs at this level
            Set<UUID> newNeighborIds = new LinkedHashSet<>();

            // Process each node in this level using pre-fetched relations
            int levelSize = queue.size();
            for (int i = 0; i < levelSize; i++) {
                UUID currentId = queue.poll();
                if (currentId == null) continue;

                // Outgoing relations (currentId = source)
                List<com.abax.memory.infrastructure.persistence.RelationEntity> outgoing =
                        bySource.getOrDefault(currentId, List.of());
                for (var rel : outgoing) {
                    addEdgeAndEnqueueBatch(rel, rel.getSourceId(), rel.getTargetId(),
                            visited, queue, edges, edgeKeys, newNeighborIds);
                }

                // Incoming relations (currentId = target)
                List<com.abax.memory.infrastructure.persistence.RelationEntity> incoming =
                        byTarget.getOrDefault(currentId, List.of());
                for (var rel : incoming) {
                    if (rel.getRelationType().isTraversableReverse()) {
                        addEdgeAndEnqueueBatch(rel, rel.getSourceId(), rel.getTargetId(),
                                visited, queue, edges, edgeKeys, newNeighborIds);
                    } else {
                        // Non-traversable incoming: show edge and entity, but don't traverse
                        String edgeKey = rel.getSourceId() + "|" + rel.getTargetId() + "|" + rel.getRelationType().name();
                        if (edgeKeys.add(edgeKey)) {
                            edges.add(new GraphEdge(rel.getSourceId(), rel.getTargetId(),
                                    rel.getRelationType().name(), Map.of()));
                        }
                        if (!visited.contains(rel.getSourceId())) {
                            visited.add(rel.getSourceId());
                            newNeighborIds.add(rel.getSourceId());
                        }
                    }
                }
            }

            // Fix 2: Batch-load ALL new neighbor entities in ONE query
            // instead of N individual findById calls
            if (!newNeighborIds.isEmpty()) {
                Map<UUID, MemoryFragmentEntity> batchLoaded = loadEntitiesBatch(newNeighborIds, tenantId);
                nodeMap.putAll(batchLoaded);
            }
        }

        // Build nodes list preserving BFS insertion order (center first)
        List<MemoryResponse> nodes = new ArrayList<>();
        for (UUID nodeId : visited) {
            MemoryFragmentEntity entity = nodeMap.get(nodeId);
            if (entity != null) {
                nodes.add(MemoryResponse.from(entity));
            }
        }

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

    /**
     * Registers a discovered edge and neighbor in batch-collecting mode.
     * Instead of directly calling findById, the new node ID is collected
     * for batch loading at the end of the BFS level.
     * Deduplicates edges using a composite key to prevent duplicates
     * when bidirectional relations are traversed from both sides.
     */
    private void addEdgeAndEnqueueBatch(
            com.abax.memory.infrastructure.persistence.RelationEntity rel,
            UUID sourceId, UUID targetId,
            Set<UUID> visited, Deque<UUID> queue,
            List<GraphEdge> edges, Set<String> edgeKeys, Set<UUID> newNeighborIds) {
        String edgeKey = sourceId + "|" + targetId + "|" + rel.getRelationType().name();
        if (edgeKeys.add(edgeKey)) {
            edges.add(new GraphEdge(sourceId, targetId, rel.getRelationType().name(), Map.of()));
        }
        // Enqueue target for next BFS level
        if (visited.add(targetId)) {
            queue.add(targetId);
            newNeighborIds.add(targetId);
        }
        // Ensure source entity is loaded for display
        if (!visited.contains(sourceId)) {
            visited.add(sourceId);
            newNeighborIds.add(sourceId);
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

            qdrantClient.upsert(qdrantCollection, entity.getId().toString(), vector, payload);
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
                qdrantClient.upsert(qdrantCollection, entity.getId().toString(), vector, payload);
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
     * Loads memory fragment entities by their IDs using a single batch query.
     * Replaces the previous N+1 findById-per-ID pattern.
     */
    private Map<String, MemoryFragmentEntity> loadEntitiesByIds(List<String> ids, String tenantId) {
        if (ids.isEmpty()) return Map.of();

        // Parse all UUIDs first, filter out invalid ones
        Set<UUID> validUuids = new LinkedHashSet<>();
        for (String idStr : ids) {
            try {
                validUuids.add(UUID.fromString(idStr));
            } catch (IllegalArgumentException e) {
                LOG.debugv("Invalid UUID in Qdrant point ID: {0}", idStr);
            }
        }
        if (validUuids.isEmpty()) return Map.of();

        // Batch query: single SQL round-trip for all IDs (Fix 2)
        Map<UUID, MemoryFragmentEntity> entityMap = loadEntitiesBatch(validUuids, tenantId);

        // Map back to original string IDs preserving insertion order
        Map<String, MemoryFragmentEntity> result = new LinkedHashMap<>();
        for (String idStr : ids) {
            try {
                UUID uuid = UUID.fromString(idStr);
                MemoryFragmentEntity entity = entityMap.get(uuid);
                if (entity != null) {
                    result.put(idStr, entity);
                }
            } catch (IllegalArgumentException e) {
                // already logged above
            }
        }
        return result;
    }

    /**
     * Batch-loads memory fragment entities by a set of UUIDs.
     * Single query: {@code SELECT ... WHERE id IN (?1)}.
     */
    private Map<UUID, MemoryFragmentEntity> loadEntitiesBatch(Set<UUID> ids, String tenantId) {
        if (ids.isEmpty()) return Map.of();
        Map<UUID, MemoryFragmentEntity> result = new LinkedHashMap<>();
        List<MemoryFragmentEntity> entities = MemoryFragmentEntity.find(
                "id IN ?1 AND tenantId = ?2 AND deletedAt IS NULL", ids, tenantId).list();
        for (MemoryFragmentEntity entity : entities) {
            result.put(entity.getId(), entity);
        }
        return result;
    }

    /**
     * Batch-loads all relations for a set of node IDs.
     * Single query: {@code SELECT ... WHERE (sourceId IN ?1 OR targetId IN ?1) AND tenantId = ?2}.
     * Replaces the N+1 findBySource/findByTarget pattern (Fix 5).
     */
    private List<com.abax.memory.infrastructure.persistence.RelationEntity> findRelationsForNodeIds(
            Set<UUID> nodeIds, String tenantId) {
        if (nodeIds.isEmpty()) return List.of();
        return com.abax.memory.infrastructure.persistence.RelationEntity.find(
                "(sourceId IN ?1 OR targetId IN ?1) AND tenantId = ?2", nodeIds, tenantId).list();
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

    // ── Consolidated Multi-Seed Graph Expansion (Fix 1, Fix 3) ─────

    /**
     * Performs a single consolidated BFS from multiple seed nodes, batch-loading
     * entities and relations at each depth level. Eliminates the redundant
     * per-seed graph expansion that caused O(topK × N) query amplification.
     *
     * <p>Each discovered graph node receives a score weighted by the maximum
     * seed score among all seeds that can reach it, multiplied by 0.7
     * (graph decay factor).</p>
     *
     * @param seeds      set of seed UUIDs to start BFS from
     * @param seedScores pre-computed vector scores for each seed
     * @param depth      max BFS depth
     * @param tenantId   tenant isolation scope
     * @return consolidated graph expansion result
     */
    private GraphExpansionResult expandGraphConsolidated(
            Set<UUID> seeds, Map<UUID, Double> seedScores, int depth, String tenantId) {
        int effectiveDepth = Math.max(1, Math.min(MAX_GRAPH_DEPTH, depth > 0 ? depth : 2));

        Set<UUID> visited = new LinkedHashSet<>();
        Map<UUID, MemoryFragmentEntity> entityMap = new LinkedHashMap<>();
        Map<UUID, Double> nodeScores = new LinkedHashMap<>(); // best seed score per node

        Deque<UUID> queue = new ArrayDeque<>();
        // Graph decay factor: connected nodes get 70% of the best connecting seed's score
        final double GRAPH_DECAY = 0.7;

        // Initialize BFS with all seeds
        for (UUID seedId : seeds) {
            if (visited.add(seedId)) {
                queue.add(seedId);
                nodeScores.put(seedId, seedScores.getOrDefault(seedId, 0.7));
            }
        }

        // Batch-load seed entities
        Map<UUID, MemoryFragmentEntity> seedEntities = loadEntitiesBatch(
                new LinkedHashSet<>(seeds), tenantId);
        entityMap.putAll(seedEntities);

        for (int level = 0; level < effectiveDepth && !queue.isEmpty(); level++) {
            Set<UUID> levelNodeIds = new LinkedHashSet<>(queue);

            // Batch-fetch ALL relations for this level (1 query)
            List<com.abax.memory.infrastructure.persistence.RelationEntity> allLevelRelations =
                    findRelationsForNodeIds(levelNodeIds, tenantId);

            // Index by source and target
            Map<UUID, List<com.abax.memory.infrastructure.persistence.RelationEntity>> bySource = new HashMap<>();
            Map<UUID, List<com.abax.memory.infrastructure.persistence.RelationEntity>> byTarget = new HashMap<>();
            for (var rel : allLevelRelations) {
                bySource.computeIfAbsent(rel.getSourceId(), k -> new ArrayList<>()).add(rel);
                byTarget.computeIfAbsent(rel.getTargetId(), k -> new ArrayList<>()).add(rel);
            }

            Set<UUID> newNeighborIds = new LinkedHashSet<>();

            int levelSize = queue.size();
            for (int i = 0; i < levelSize; i++) {
                UUID currentId = queue.poll();
                if (currentId == null) continue;

                double currentScore = nodeScores.getOrDefault(currentId, 0.5);

                // Outgoing: current → neighbor
                for (var rel : bySource.getOrDefault(currentId, List.of())) {
                    UUID neighborId = rel.getTargetId();
                    if (visited.add(neighborId)) {
                        queue.add(neighborId);
                        newNeighborIds.add(neighborId);
                        nodeScores.put(neighborId, currentScore * GRAPH_DECAY);
                    }
                }

                // Incoming (reverse-traversable): neighbor → current
                for (var rel : byTarget.getOrDefault(currentId, List.of())) {
                    if (rel.getRelationType().isTraversableReverse()) {
                        UUID neighborId = rel.getSourceId();
                        if (visited.add(neighborId)) {
                            queue.add(neighborId);
                            newNeighborIds.add(neighborId);
                            nodeScores.put(neighborId, currentScore * GRAPH_DECAY);
                        }
                    } else {
                        // Non-traversable incoming: load entity but don't traverse
                        UUID sourceId = rel.getSourceId();
                        if (!visited.contains(sourceId)) {
                            visited.add(sourceId);
                            newNeighborIds.add(sourceId);
                            nodeScores.put(sourceId, currentScore * GRAPH_DECAY);
                        }
                    }
                }
            }

            // Batch-load all newly discovered entities at this level (1 query)
            if (!newNeighborIds.isEmpty()) {
                Map<UUID, MemoryFragmentEntity> batchLoaded = loadEntitiesBatch(newNeighborIds, tenantId);
                entityMap.putAll(batchLoaded);
            }
        }

        // Build result: all visited nodes (excluding seeds already in vector results)
        // with their pre-computed graph scores
        Map<UUID, Double> graphNodes = new LinkedHashMap<>();
        for (UUID nodeId : visited) {
            if (!seeds.contains(nodeId) && entityMap.containsKey(nodeId)) {
                graphNodes.put(nodeId, nodeScores.getOrDefault(nodeId, 0.5));
            }
        }

        return new GraphExpansionResult(graphNodes, entityMap);
    }

    /**
     * Internal holder for consolidated graph expansion results.
     * {@code nodes} maps graph-discovered node IDs to their computed scores,
     * and {@code entityMap} holds the loaded JPA entities (already filtered by tenant).
     */
    private record GraphExpansionResult(
            Map<UUID, Double> nodes,
            Map<UUID, MemoryFragmentEntity> entityMap
    ) {
    }
}

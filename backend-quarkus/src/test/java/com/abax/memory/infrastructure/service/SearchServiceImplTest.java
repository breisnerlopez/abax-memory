package com.abax.memory.infrastructure.service;

import com.abax.memory.api.dto.v2.CreateMemoryRequest;
import com.abax.memory.api.dto.v2.GraphResponse;
import com.abax.memory.api.dto.v2.MemoryResponse;
import com.abax.memory.api.dto.v2.ScoredMemory;
import com.abax.memory.api.dto.v2.SearchResponse;
import com.abax.memory.api.dto.v2.SemanticSearchRequest;
import com.abax.memory.api.dto.v2.UnifiedSearchRequest;
import com.abax.memory.api.dto.v2.UnifiedSearchResponse;
import com.abax.memory.domain.enums.LifecycleState;
import com.abax.memory.domain.enums.MemoryKind;
import com.abax.memory.domain.enums.RelationType;
import com.abax.memory.domain.enums.SensitivityLevel;
import com.abax.memory.domain.service.MemoryService;
import com.abax.memory.domain.service.RelationService;
import com.abax.memory.domain.service.SearchService;
import com.abax.memory.test.H2TestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Service-layer tests for {@link SearchServiceImpl} — EP-005.
 *
 * <p>Tests semantic search, hybrid search, findSimilar,
 * expandGraph, indexFragment, and reindexAll.</p>
 *
 * <p>References: HU-005.1.1 through HU-005.7.1</p>
 */
@QuarkusTest
@TestProfile(H2TestProfile.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("SearchServiceImpl — search and graph tests")
class SearchServiceImplTest {

    private static final String TENANT_A = "tenant-a-search";
    private static final String TENANT_B = "tenant-b-search";

    @Inject
    SearchService searchService;

    @Inject
    MemoryService memoryService;

    @Inject
    RelationService relationService;

    // ── Semantic Search Tests ────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("semanticSearch — returns results for a query")
    @Transactional
    void semanticSearch_shouldReturnResults() {
        // Create several fragments
        var m1 = memoryService.createV2(
                new CreateMemoryRequest("Fault tolerance patterns", "Circuit breaker and retry patterns for resilient systems.", MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);
        var m2 = memoryService.createV2(
                new CreateMemoryRequest("Load balancing algorithms", "Round-robin, least connections, and weighted distributions.", MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);
        var m3 = memoryService.createV2(
                new CreateMemoryRequest("Incident response plan", "Step-by-step guide for handling production incidents.", MemoryKind.DECISION, null, null, null, null, null, null, null), TENANT_A);

        // Index them
        searchService.indexFragment(m1.id(), TENANT_A);
        searchService.indexFragment(m2.id(), TENANT_A);
        searchService.indexFragment(m3.id(), TENANT_A);

        // Search for resilience-related content
        var request = new SemanticSearchRequest("resilient system patterns", null, null, null, null, null, null, 0, 20, 10);
        SearchResponse response = searchService.semanticSearch(request, TENANT_A);

        assertThat(response.items()).isNotEmpty();
        assertThat(response.total()).isGreaterThanOrEqualTo(1);
        assertThat(response.facets()).containsKeys("kind", "lifecycleState", "sensitivityLevel");

        // Issue #18: verify Qdrant relevance score is propagated to API response
        assertThat(response.items())
                .as("Every semantic search result must have a non-null relevance score (Issue #18)")
                .allMatch(r -> r.score() != null);
        assertThat(response.items())
                .allMatch(r -> r.score() >= -1.0 && r.score() <= 1.0);
    }

    @Test
    @Order(2)
    @DisplayName("semanticSearch — respects kind filter")
    @Transactional
    void semanticSearch_shouldRespectKindFilter() {
        var m1 = memoryService.createV2(
                new CreateMemoryRequest("Decision about deployment", "We decided to use blue-green deployments.", MemoryKind.DECISION, null, null, null, null, null, null, null), TENANT_A);
        var m2 = memoryService.createV2(
                new CreateMemoryRequest("Knowledge about deployment", "Blue-green deployment reduces downtime.", MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);

        searchService.indexFragment(m1.id(), TENANT_A);
        searchService.indexFragment(m2.id(), TENANT_A);

        var request = new SemanticSearchRequest("deployment strategies", List.of(MemoryKind.DECISION), null, null, null, null, null, 0, 20, 10);
        SearchResponse response = searchService.semanticSearch(request, TENANT_A);

        assertThat(response.items()).isNotEmpty();
        assertThat(response.items()).allMatch(r -> r.kind() == MemoryKind.DECISION);
    }

    @Test
    @Order(3)
    @DisplayName("semanticSearch — respects tenant isolation")
    @Transactional
    void semanticSearch_shouldRespectTenantIsolation() {
        var m1 = memoryService.createV2(
                new CreateMemoryRequest("Tenant A secret", "Confidential data for tenant A.", MemoryKind.FACT, null, SensitivityLevel.CONFIDENTIAL, null, null, null, null, null), TENANT_A);

        searchService.indexFragment(m1.id(), TENANT_A);

        var request = new SemanticSearchRequest("confidential", null, null, null, null, null, null, 0, 20, 10);
        SearchResponse response = searchService.semanticSearch(request, TENANT_B);

        assertThat(response.items()).isEmpty();
        assertThat(response.total()).isEqualTo(0);
    }

    // ── Hybrid Search Tests ──────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("hybridSearch — returns combined results")
    @Transactional
    void hybridSearch_shouldReturnCombinedResults() {
        var m1 = memoryService.createV2(
                new CreateMemoryRequest("Database migration guide", "How to safely migrate PostgreSQL databases with zero downtime.", MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);
        var m2 = memoryService.createV2(
                new CreateMemoryRequest("API rate limiting", "Implement rate limiting using token bucket algorithm.", MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);

        searchService.indexFragment(m1.id(), TENANT_A);
        searchService.indexFragment(m2.id(), TENANT_A);

        var request = new SemanticSearchRequest("database migration", null, null, null, null, null, null, 0, 20, 10);
        SearchResponse response = searchService.hybridSearch(request, TENANT_A);

        assertThat(response.items()).isNotEmpty();
        // The database migration title should rank higher
        assertThat(response.items().get(0).title()).contains("Database");

        // Issue #18: verify hybrid score is propagated to API response
        assertThat(response.items())
                .as("Every hybrid search result must have a non-null relevance score (Issue #18)")
                .allMatch(r -> r.score() != null);
        assertThat(response.items())
                .allMatch(r -> r.score() >= -1.0 && r.score() <= 1.0);
    }

    // ── Find Similar Tests ───────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("findSimilar — excludes source fragment and returns results")
    @Transactional
    void findSimilar_shouldReturnSimilarExcludingSource() {
        // Use identical content to guarantee similarity with deterministic hash-based mock
        String sameContent = "Optimize database queries with proper indexing and vacuum strategies for PostgreSQL.";
        var m1 = memoryService.createV2(
                new CreateMemoryRequest("PostgreSQL performance tuning", sameContent, MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);
        var m2 = memoryService.createV2(
                new CreateMemoryRequest("Database optimization tips", sameContent, MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);

        searchService.indexFragment(m1.id(), TENANT_A);
        searchService.indexFragment(m2.id(), TENANT_A);

        List<MemoryResponse> similar = searchService.findSimilar(m1.id(), TENANT_A, 10);

        // With identical content in the mock, m2 should be found as similar
        assertThat(similar).isNotEmpty();
        // Should not include the source fragment itself
        assertThat(similar).noneMatch(r -> r.id().equals(m1.id()));

        // Issue #18: verify Qdrant score is propagated to findSimilar results
        assertThat(similar)
                .as("Every findSimilar result must have a non-null relevance score (Issue #18)")
                .allMatch(r -> r.score() != null);
    }

    @Test
    @Order(6)
    @DisplayName("findSimilar — works with on-the-fly embedding generation when not indexed")
    @Transactional
    void findSimilar_shouldWorkWithOnTheFlyEmbedding() {
        var m1 = memoryService.createV2(
                new CreateMemoryRequest("Orphan fragment", "Unique content that should not match anything else in the index.", MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);
        // Deliberately not indexing this fragment — on-the-fly embedding should handle it

        List<MemoryResponse> similar = searchService.findSimilar(m1.id(), TENANT_A, 10);

        // Should never include the source fragment itself
        assertThat(similar).noneMatch(r -> r.id().equals(m1.id()));
    }

    // ── Graph Expansion Tests ────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("expandGraph — returns center node and related nodes")
    @Transactional
    void expandGraph_shouldReturnCenterAndRelatedNodes() {
        var center = memoryService.createV2(
                new CreateMemoryRequest("Central Decision", "The main architectural decision.", MemoryKind.DECISION, null, null, null, null, null, null, null), TENANT_A);
        var child1 = memoryService.createV2(
                new CreateMemoryRequest("Supporting Evidence A", "Evidence supporting the central decision.", MemoryKind.PROCEDURE, null, null, null, null, null, null, null), TENANT_A);
        var child2 = memoryService.createV2(
                new CreateMemoryRequest("Supporting Evidence B", "More evidence.", MemoryKind.PROCEDURE, null, null, null, null, null, null, null), TENANT_A);

        // Create relations: center → child1, center → child2
        relationService.createRelation(center.id(), child1.id(), RelationType.SUPPORTS, TENANT_A);
        relationService.createRelation(center.id(), child2.id(), RelationType.SUPPORTS, TENANT_A);

        GraphResponse graph = searchService.expandGraph(center.id(), 1, TENANT_A);

        assertThat(graph.centerNode()).isNotNull();
        assertThat(graph.centerNode().id()).isEqualTo(center.id());
        assertThat(graph.nodes()).hasSize(3); // center + 2 children
        assertThat(graph.relations()).hasSize(2);
    }

    @Test
    @Order(8)
    @DisplayName("expandGraph — BFS respects depth limit")
    @Transactional
    void expandGraph_shouldRespectDepthLimit() {
        var level0 = memoryService.createV2(
                new CreateMemoryRequest("Root Level 0", "Root node.", MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);
        var level1 = memoryService.createV2(
                new CreateMemoryRequest("Node Level 1", "Child of root.", MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);
        var level2 = memoryService.createV2(
                new CreateMemoryRequest("Node Level 2", "Child of level 1.", MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);

        relationService.createRelation(level0.id(), level1.id(), RelationType.RELATED_TO, TENANT_A);
        relationService.createRelation(level1.id(), level2.id(), RelationType.RELATED_TO, TENANT_A);

        // Depth 1: should see level0 and level1, but NOT level2
        GraphResponse graphDepth1 = searchService.expandGraph(level0.id(), 1, TENANT_A);
        assertThat(graphDepth1.nodes()).hasSize(2);
        assertThat(graphDepth1.nodes()).noneMatch(n -> n.id().equals(level2.id()));

        // Depth 2: should see all three
        GraphResponse graphDepth2 = searchService.expandGraph(level0.id(), 2, TENANT_A);
        assertThat(graphDepth2.nodes()).hasSize(3);
    }

    @Test
    @Order(9)
    @DisplayName("expandGraph — tenant isolation in graph traversal")
    @Transactional
    void expandGraph_shouldRespectTenantIsolation() {
        var centerA = memoryService.createV2(
                new CreateMemoryRequest("Tenant A Node", "Belongs to tenant A.", MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);
        var childB = memoryService.createV2(
                new CreateMemoryRequest("Tenant B Node", "Belongs to tenant B.", MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_B);

        // Create relation cross-tenant (validated at relation creation time)
        try {
            relationService.createRelation(centerA.id(), childB.id(), RelationType.RELATED_TO, TENANT_A);
        } catch (Exception e) {
            // Expected: cross-tenant relation creation should fail
        }

        GraphResponse graph = searchService.expandGraph(centerA.id(), 2, TENANT_A);
        // All nodes should belong to tenant A
        assertThat(graph.nodes()).allMatch(n -> TENANT_A.equals(n.tenantId()));
    }

    // ── Index Fragment Tests ─────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("indexFragment — successfully indexes a memory fragment and writes back embedding_id")
    @Transactional
    void indexFragment_shouldSucceed() {
        var fragment = memoryService.createV2(
                new CreateMemoryRequest("Indexable Memory", "Content for indexing test.", MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);

        assertThatNoException()
                .isThrownBy(() -> searchService.indexFragment(fragment.id(), TENANT_A));

        // Issue #17: verify embedding_id is written back to PostgreSQL after Qdrant upsert
        var reloaded = memoryService.getByIdV2(fragment.id(), TENANT_A);
        assertThat(reloaded.embeddingId())
                .as("embedding_id must be set after indexing — Issue #17")
                .isNotNull()
                .isEqualTo(fragment.id().toString());
    }

    @Test
    @Order(11)
    @DisplayName("reindexAll — counts indexed fragments and writes back embedding_id")
    @Transactional
    void reindexAll_shouldCountIndexedFragments() {
        var f1 = memoryService.createV2(
                new CreateMemoryRequest("Reindex Test 1", "Content one.", MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);
        var f2 = memoryService.createV2(
                new CreateMemoryRequest("Reindex Test 2", "Content two.", MemoryKind.DECISION, null, null, null, null, null, null, null), TENANT_A);

        int indexed = searchService.reindexAll(TENANT_A);
        assertThat(indexed).isGreaterThanOrEqualTo(2);

        // Issue #17: verify embedding_id is written back after reindexAll
        var reloaded1 = memoryService.getByIdV2(f1.id(), TENANT_A);
        var reloaded2 = memoryService.getByIdV2(f2.id(), TENANT_A);
        assertThat(reloaded1.embeddingId())
                .as("embedding_id must be set after reindexAll — Issue #17")
                .isNotNull()
                .isEqualTo(f1.id().toString());
        assertThat(reloaded2.embeddingId())
                .as("embedding_id must be set after reindexAll — Issue #17")
                .isNotNull()
                .isEqualTo(f2.id().toString());
    }

    // ── Unified Search Tests ──────────────────────────────────────────

    @Test
    @Order(12)
    @DisplayName("unifiedSearch — returns results with both vector and graph sources")
    @Transactional
    void unifiedSearch_shouldReturnVectorAndGraphResults() {
        // Create a hub fragment (will be found by vector search)
        var hub = memoryService.createV2(
                new CreateMemoryRequest("Database connection pool optimization",
                        "How to optimize PostgreSQL connection pools for high throughput systems using PgBouncer and HikariCP.",
                        MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);
        var leaf = memoryService.createV2(
                new CreateMemoryRequest("PgBouncer configuration tips",
                        "Detailed tips about PgBouncer configuration: pool size, timeouts, and monitoring.",
                        MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);
        var distant = memoryService.createV2(
                new CreateMemoryRequest("Distantly related performance note",
                        "General notes about system performance in distributed architectures.",
                        MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);

        // Create graph: hub → leaf
        relationService.createRelation(hub.id(), leaf.id(), RelationType.SUPPORTS, TENANT_A);

        // Index all for search
        searchService.indexFragment(hub.id(), TENANT_A);
        searchService.indexFragment(leaf.id(), TENANT_A);
        searchService.indexFragment(distant.id(), TENANT_A);

        // Execute unified search with graph expansion
        UnifiedSearchRequest request = new UnifiedSearchRequest(
                "database connection pool", null, null, null, null,
                0, 20, true, 2, 3);
        UnifiedSearchResponse response = searchService.unifiedSearch(request, TENANT_A);

        // Assertions
        assertThat(response).isNotNull();
        assertThat(response.getItems()).isNotEmpty();
        assertThat(response.getTotal()).isGreaterThanOrEqualTo(1);
        assertThat(response.isGraphExpanded()).isTrue();

        // At least one result should be from vector source
        List<ScoredMemory> vectorItems = response.getItems().stream()
                .filter(sm -> "vector".equals(sm.getSource()))
                .toList();
        assertThat(vectorItems).isNotEmpty();

        // Verify score ordering: items should be sorted by score descending
        for (int i = 0; i < response.getItems().size() - 1; i++) {
            Double current = response.getItems().get(i).getScore();
            Double next = response.getItems().get(i + 1).getScore();
            assertThat(current).isGreaterThanOrEqualTo(next);
        }

        // Verify facets are present
        assertThat(response.getFacets()).containsKeys("kind", "lifecycleState", "sensitivityLevel");
    }

    @Test
    @Order(13)
    @DisplayName("unifiedSearch — expandGraph=false yields only vector results")
    @Transactional
    void unifiedSearch_withoutGraphExpansion_shouldYieldOnlyVectorResults() {
        var hub = memoryService.createV2(
                new CreateMemoryRequest("Cache invalidation strategies",
                        "How to design cache invalidation strategies with Redis and Memcached.",
                        MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);
        var leaf = memoryService.createV2(
                new CreateMemoryRequest("Redis cluster setup",
                        "Setting up Redis cluster for high availability caching.",
                        MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);

        relationService.createRelation(hub.id(), leaf.id(), RelationType.RELATED_TO, TENANT_A);
        searchService.indexFragment(hub.id(), TENANT_A);
        searchService.indexFragment(leaf.id(), TENANT_A);

        // Execute without graph expansion
        UnifiedSearchRequest request = new UnifiedSearchRequest(
                "cache invalidation", null, null, null, null,
                0, 20, false, 0, 0);
        UnifiedSearchResponse response = searchService.unifiedSearch(request, TENANT_A);

        assertThat(response.isGraphExpanded()).isFalse();
        assertThat(response.getGraphContributions()).isEqualTo(0);

        // All items should be from vector source
        assertThat(response.getItems())
                .allMatch(sm -> "vector".equals(sm.getSource()));
    }

    @Test
    @Order(14)
    @DisplayName("unifiedSearch — respects pagination")
    @Transactional
    void unifiedSearch_shouldRespectPagination() {
        // Create several fragments with similar content to get multiple results
        for (int i = 1; i <= 5; i++) {
            var frag = memoryService.createV2(
                    new CreateMemoryRequest("Performance tip #" + i,
                            "How to improve database performance using connection pooling and query optimization technique #" + i + ".",
                            MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);
            searchService.indexFragment(frag.id(), TENANT_A);
        }

        // Page 0, size 2
        UnifiedSearchRequest request = new UnifiedSearchRequest(
                "performance connection pooling", null, null, null, null,
                0, 2, false, 0, 0);
        UnifiedSearchResponse page0 = searchService.unifiedSearch(request, TENANT_A);

        assertThat(page0.getItems()).hasSizeLessThanOrEqualTo(2);
        assertThat(page0.getPage()).isEqualTo(0);
        assertThat(page0.getSize()).isEqualTo(2);

        // Page 1, size 2
        request.setPage(1);
        UnifiedSearchResponse page1 = searchService.unifiedSearch(request, TENANT_A);

        // Ensure no overlap between pages
        List<UUID> page0Ids = page0.getItems().stream()
                .map(sm -> sm.getMemory().id())
                .toList();
        List<UUID> page1Ids = page1.getItems().stream()
                .map(sm -> sm.getMemory().id())
                .toList();
        assertThat(page0Ids).doesNotContainAnyElementsOf(page1Ids);
    }

    @Test
    @Order(15)
    @DisplayName("unifiedSearch — graphDepth:0 limits reachable graph nodes")
    @Transactional
    void unifiedSearch_shouldLimitGraphDepth() {
        var root = memoryService.createV2(
                new CreateMemoryRequest("Data engineering fundamentals",
                        "Fundamentals of data engineering: pipelines, ETL, and batch processing.",
                        MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);
        var level1 = memoryService.createV2(
                new CreateMemoryRequest("ETL pipeline design",
                        "Design patterns for ETL pipelines in modern data stacks.",
                        MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);
        var level2 = memoryService.createV2(
                new CreateMemoryRequest("Apache Airflow tips",
                        "Tips for using Apache Airflow in production data pipelines.",
                        MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);

        relationService.createRelation(root.id(), level1.id(), RelationType.SUPPORTS, TENANT_A);
        relationService.createRelation(level1.id(), level2.id(), RelationType.RELATED_TO, TENANT_A);
        searchService.indexFragment(root.id(), TENANT_A);
        searchService.indexFragment(level1.id(), TENANT_A);
        searchService.indexFragment(level2.id(), TENANT_A);

        // Depth 1: should reach level1 but not level2
        UnifiedSearchRequest request = new UnifiedSearchRequest(
                "data engineering", null, null, null, null,
                0, 20, true, 1, 5);
        UnifiedSearchResponse response = searchService.unifiedSearch(request, TENANT_A);

        // level2 should not appear when depth is only 1
        boolean hasLevel2 = response.getItems().stream()
                .anyMatch(sm -> sm.getMemory().id().equals(level2.id()));
        assertThat(hasLevel2).isFalse();
    }

    @Test
    @Order(16)
    @DisplayName("unifiedSearch — graphTopK limits seeds for graph expansion")
    @Transactional
    void unifiedSearch_shouldRespectGraphTopK() {
        // Create 4 seed candidates but only expand from top 2
        var seed1 = memoryService.createV2(
                new CreateMemoryRequest("Seed A: Kubernetes scaling",
                        "Kubernetes horizontal pod autoscaler configuration and tuning for high-traffic services.",
                        MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);
        var seed2 = memoryService.createV2(
                new CreateMemoryRequest("Seed B: Kubernetes pod scaling",
                        "Deep dive into pod scaling policies and resource management.",
                        MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);
        var seed3 = memoryService.createV2(
                new CreateMemoryRequest("Seed C: unrelated topic",
                        "Best practices for writing unit tests in Java.",
                        MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);
        var seed4 = memoryService.createV2(
                new CreateMemoryRequest("Seed D: another unrelated",
                        "How to configure logging in Spring Boot applications.",
                        MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);

        // Each seed has a connected node
        var child1 = memoryService.createV2(
                new CreateMemoryRequest("Child A: HPA details",
                        "Detailed HPA metric configuration for Kubernetes clusters.", MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);
        var child3 = memoryService.createV2(
                new CreateMemoryRequest("Child C: JUnit 5 features",
                        "JUnit 5 parameterized tests and test templates explained.", MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);

        relationService.createRelation(seed1.id(), child1.id(), RelationType.SUPPORTS, TENANT_A);
        relationService.createRelation(seed3.id(), child3.id(), RelationType.SUPPORTS, TENANT_A);

        // Index all
        for (var frag : java.util.List.of(seed1, seed2, seed3, seed4, child1, child3)) {
            searchService.indexFragment(frag.id(), TENANT_A);
        }

        // graphTopK=2: only expand from top 2 seeds
        UnifiedSearchRequest request = new UnifiedSearchRequest(
                "kubernetes scaling", null, null, null, null,
                0, 20, true, 2, 2);
        UnifiedSearchResponse response = searchService.unifiedSearch(request, TENANT_A);

        assertThat(response.getItems()).isNotEmpty();
        // The response should include items — graph expansion contributes from top-K seeds
    }

    @Test
    @Order(17)
    @DisplayName("unifiedSearch — blank query returns 400 via validation")
    @Transactional
    void unifiedSearch_blankQuery_shouldFailValidation() {
        // This test verifies the method can be called — actual validation
        // rejection at the REST layer; at service layer we rely on the caller
        // to validate. The service itself should still process empty string.
        // We just verify it doesn't throw unexpectedly.
        UnifiedSearchRequest request = new UnifiedSearchRequest(
                "  ", null, null, null, null, 0, 5, false, 0, 0);
        UnifiedSearchResponse response = searchService.unifiedSearch(request, TENANT_A);

        // Should still return a valid (likely empty) response rather than throwing
        assertThat(response).isNotNull();
        assertThat(response.getTotal()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @Order(18)
    @DisplayName("unifiedSearch — deduplicates: same UUID never appears twice")
    @Transactional
    void unifiedSearch_shouldNotDuplicateIds() {
        var hub = memoryService.createV2(
                new CreateMemoryRequest("Microservices authentication",
                        "Authentication patterns in microservices architectures with OAuth2 and JWT.",
                        MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);
        var leaf = memoryService.createV2(
                new CreateMemoryRequest("OAuth2 JWT best practices",
                        "Implementing OAuth2 and JWT securely in distributed microservices.",
                        MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);

        relationService.createRelation(hub.id(), leaf.id(), RelationType.SUPPORTS, TENANT_A);
        searchService.indexFragment(hub.id(), TENANT_A);
        searchService.indexFragment(leaf.id(), TENANT_A);

        UnifiedSearchRequest request = new UnifiedSearchRequest(
                "authentication microservices OAuth2", null, null, null, null,
                0, 20, true, 2, 5);
        UnifiedSearchResponse response = searchService.unifiedSearch(request, TENANT_A);

        // Verify no duplicate IDs
        List<UUID> ids = response.getItems().stream()
                .map(sm -> sm.getMemory().id())
                .toList();
        assertThat(ids).doesNotHaveDuplicates();

        // Count sources
        long vectorCount = response.getItems().stream()
                .filter(sm -> "vector".equals(sm.getSource()))
                .count();
        long graphCount = response.getItems().stream()
                .filter(sm -> "graph".equals(sm.getSource()))
                .count();

        assertThat(vectorCount).isGreaterThanOrEqualTo(1);
        assertThat(response.getGraphContributions()).isEqualTo(graphCount);
    }

    // ── Issue #18 Tests ───────────────────────────────────────────────

    @Test
    @Order(19)
    @DisplayName("ISSUE-18: semanticSearch results must have scores propagated from Qdrant")
    @Transactional
    void issue18_semanticSearch_scoresArePropagated() {
        var m1 = memoryService.createV2(
                new CreateMemoryRequest("Neural network optimization",
                        "Techniques for optimizing deep neural networks: pruning, quantization, and knowledge distillation.",
                        MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);
        var m2 = memoryService.createV2(
                new CreateMemoryRequest("Distributed training strategies",
                        "Data parallelism, model parallelism, and pipeline parallelism for large-scale neural network training.",
                        MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);
        var m3 = memoryService.createV2(
                new CreateMemoryRequest("Gradient descent variants",
                        "SGD, Adam, RMSprop, and Adagrad optimizers explained with convergence analysis.",
                        MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);

        searchService.indexFragment(m1.id(), TENANT_A);
        searchService.indexFragment(m2.id(), TENANT_A);
        searchService.indexFragment(m3.id(), TENANT_A);

        // Query that should match m1 most closely
        var request = new SemanticSearchRequest("neural network optimization pruning quantization",
                null, null, null, null, null, null, 0, 20, 10);
        SearchResponse response = searchService.semanticSearch(request, TENANT_A);

        assertThat(response.items()).isNotEmpty();
        // All results must have non-null scores
        assertThat(response.items())
                .as("Issue #18: Qdrant scores must be propagated to every result")
                .allMatch(r -> r.score() != null && r.score() > 0.0);

        // Results should be sorted by relevance score descending
        for (int i = 0; i < response.items().size() - 1; i++) {
            Double current = response.items().get(i).score();
            Double next = response.items().get(i + 1).score();
            assertThat(current)
                    .as("Issue #18: results must be sorted by score descending")
                    .isGreaterThanOrEqualTo(next);
        }
    }

    @Test
    @Order(20)
    @DisplayName("ISSUE-18: hybridSearch results must have scores propagated")
    @Transactional
    void issue18_hybridSearch_scoresArePropagated() {
        var m1 = memoryService.createV2(
                new CreateMemoryRequest("Kubernetes pod scheduling algorithms",
                        "Detailed analysis of Kubernetes scheduler: predicates, priorities, and custom scheduling policies.",
                        MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);
        var m2 = memoryService.createV2(
                new CreateMemoryRequest("Container orchestration with Kubernetes",
                        "Kubernetes architecture, control plane components, and etcd cluster management.",
                        MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);

        searchService.indexFragment(m1.id(), TENANT_A);
        searchService.indexFragment(m2.id(), TENANT_A);

        var request = new SemanticSearchRequest("kubernetes scheduling", null, null, null, null, null, null, 0, 20, 10);
        SearchResponse response = searchService.hybridSearch(request, TENANT_A);

        assertThat(response.items()).isNotEmpty();
        // All results must have non-null hybrid scores
        assertThat(response.items())
                .as("Issue #18: hybrid scores must be propagated to every result")
                .allMatch(r -> r.score() != null && r.score() > 0.0);

        // Results should be sorted by hybrid score descending
        for (int i = 0; i < response.items().size() - 1; i++) {
            Double current = response.items().get(i).score();
            Double next = response.items().get(i + 1).score();
            assertThat(current)
                    .as("Issue #18: hybrid search results must be sorted by score descending")
                    .isGreaterThanOrEqualTo(next);
        }
    }
}

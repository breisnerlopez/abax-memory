package com.abax.memory.infrastructure.service;

import com.abax.memory.api.dto.v2.CreateMemoryRequest;
import com.abax.memory.api.dto.v2.GraphResponse;
import com.abax.memory.api.dto.v2.MemoryResponse;
import com.abax.memory.api.dto.v2.SearchResponse;
import com.abax.memory.api.dto.v2.SemanticSearchRequest;
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
    @DisplayName("indexFragment — successfully indexes a memory fragment")
    @Transactional
    void indexFragment_shouldSucceed() {
        var fragment = memoryService.createV2(
                new CreateMemoryRequest("Indexable Memory", "Content for indexing test.", MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);

        assertThatNoException()
                .isThrownBy(() -> searchService.indexFragment(fragment.id(), TENANT_A));
    }

    @Test
    @Order(11)
    @DisplayName("reindexAll — counts indexed fragments")
    @Transactional
    void reindexAll_shouldCountIndexedFragments() {
        var f1 = memoryService.createV2(
                new CreateMemoryRequest("Reindex Test 1", "Content one.", MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);
        var f2 = memoryService.createV2(
                new CreateMemoryRequest("Reindex Test 2", "Content two.", MemoryKind.DECISION, null, null, null, null, null, null, null), TENANT_A);

        int indexed = searchService.reindexAll(TENANT_A);
        assertThat(indexed).isGreaterThanOrEqualTo(2);
    }
}

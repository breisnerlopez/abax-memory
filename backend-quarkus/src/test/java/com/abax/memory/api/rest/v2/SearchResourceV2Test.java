package com.abax.memory.api.rest.v2;

import com.abax.memory.test.H2TestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.containsString;

/**
 * Integration tests for {@link SearchResourceV2} — search, graph,
 * and relation endpoints.
 *
 * <p>Uses H2 in-memory database and mock tenant auth via
 * {@code X-Tenant-Id} header.</p>
 *
 * <p>References: HU-005.1.1 through HU-005.9.1, HU-001.8.1, HU-001.8.2</p>
 */
@QuarkusTest
@TestProfile(H2TestProfile.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("SearchResourceV2 — search and graph endpoints")
class SearchResourceV2Test {

    private static final String TENANT_A = "tenant-a-search-resource";
    private static final String TENANT_B = "tenant-b-search-resource";

    // ── Semantic Search Tests ────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("POST /api/v2/search/semantic — returns 200 with search results")
    void semanticSearch_returns200() {
        // First create a fragment via the memories endpoint
        String id = createMemory(TENANT_A, "Semantic Search Target",
                "Content about semantic search testing with vectors.",
                "FACT", "INTERNAL");

        given()
                .header("X-Tenant-Id", TENANT_A)
                .contentType(ContentType.JSON)
                .body(Map.of("query", "semantic search vectors"))
                .when()
                .post("/api/v2/search/semantic")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("total", notNullValue())
                .body("facets", notNullValue());
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/v2/search/semantic — returns 400 for blank query")
    void semanticSearch_blankQuery_returns400() {
        given()
                .header("X-Tenant-Id", TENANT_A)
                .contentType(ContentType.JSON)
                .body(Map.of("query", "  "))
                .when()
                .post("/api/v2/search/semantic")
                .then()
                .statusCode(400);
    }

    // ── Hybrid Search Tests ──────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("POST /api/v2/search/hybrid — returns 200 with combined results")
    void hybridSearch_returns200() {
        createMemory(TENANT_A, "Hybrid Search Test",
                "Content for testing hybrid search combining vectors and keywords.",
                "FACT", "INTERNAL");

        given()
                .header("X-Tenant-Id", TENANT_A)
                .contentType(ContentType.JSON)
                .body(Map.of("query", "hybrid keyword vectors"))
                .when()
                .post("/api/v2/search/hybrid")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("total", notNullValue());
    }

    // ── Similar Search Tests ─────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("GET /api/v2/search/similar/{id} — returns 200 with similar fragments")
    void findSimilar_returns200() {
        String id = createMemory(TENANT_A, "Source For Similar",
                "Source content for finding similar memories in the system.",
                "FACT", "INTERNAL");

        given()
                .header("X-Tenant-Id", TENANT_A)
                .queryParam("limit", 5)
                .when()
                .get("/api/v2/search/similar/" + id)
                .then()
                .statusCode(200);
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/v2/search/similar/{id} — returns 404 for non-existent fragment")
    void findSimilar_nonExistent_returns404() {
        given()
                .header("X-Tenant-Id", TENANT_A)
                .when()
                .get("/api/v2/search/similar/" + UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    // ── Graph Expansion Tests ────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("GET /api/v2/graph/{id} — returns 200 with graph response")
    void expandGraph_returns200() {
        String centerId = createMemory(TENANT_A, "Graph Center Node",
                "Central node for graph test.", "DECISION", "INTERNAL");
        String childId = createMemory(TENANT_A, "Graph Child Node",
                "Child node connected to center.", "PROCEDURE", "INTERNAL");

        // Create relation between them
        createRelation(TENANT_A, centerId, childId, "SUPPORTS");

        given()
                .header("X-Tenant-Id", TENANT_A)
                .queryParam("depth", 2)
                .when()
                .get("/api/v2/graph/" + centerId)
                .then()
                .statusCode(200)
                .body("centerNode", notNullValue())
                .body("centerNode.id", equalTo(centerId))
                .body("relations", notNullValue())
                .body("nodes", notNullValue());
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/v2/graph/{id} — returns 404 for non-existent fragment")
    void expandGraph_nonExistent_returns404() {
        given()
                .header("X-Tenant-Id", TENANT_A)
                .when()
                .get("/api/v2/graph/" + UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    // ── Relation CRUD Tests ──────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("POST /api/v2/relations — returns 201 with created relation")
    void createRelation_returns201() {
        String sourceId = createMemory(TENANT_A, "Relation Source",
                "Source fragment for relation.", "FACT", "INTERNAL");
        String targetId = createMemory(TENANT_A, "Relation Target",
                "Target fragment for relation.", "FACT", "INTERNAL");

        given()
                .header("X-Tenant-Id", TENANT_A)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "sourceId", sourceId,
                        "targetId", targetId,
                        "relationType", "RELATED_TO"
                ))
                .when()
                .post("/api/v2/relations")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("sourceId", equalTo(sourceId))
                .body("targetId", equalTo(targetId))
                .body("relationType", equalTo("RELATED_TO"));
    }

    @Test
    @Order(9)
    @DisplayName("POST /api/v2/relations — returns 400 for self-relation")
    void createRelation_selfRelation_returns400() {
        String id = createMemory(TENANT_A, "Self-Relation Test",
                "Cannot relate to itself.", "FACT", "INTERNAL");

        given()
                .header("X-Tenant-Id", TENANT_A)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "sourceId", id,
                        "targetId", id,
                        "relationType", "RELATED_TO"
                ))
                .when()
                .post("/api/v2/relations")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(10)
    @DisplayName("POST /api/v2/relations — returns 404 for non-existent target")
    void createRelation_nonExistentTarget_returns404() {
        String sourceId = createMemory(TENANT_A, "Source For Missing Target",
                "Source with non-existent target.", "FACT", "INTERNAL");

        given()
                .header("X-Tenant-Id", TENANT_A)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "sourceId", sourceId,
                        "targetId", UUID.randomUUID().toString(),
                        "relationType", "RELATED_TO"
                ))
                .when()
                .post("/api/v2/relations")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(11)
    @DisplayName("GET /api/v2/relations/{id} — returns 200 with relations list")
    void getRelations_returns200() {
        String sourceId = createMemory(TENANT_A, "Get Relations Source",
                "Source for listing relations.", "FACT", "INTERNAL");
        String targetId = createMemory(TENANT_A, "Get Relations Target",
                "Target for listing relations.", "FACT", "INTERNAL");

        createRelation(TENANT_A, sourceId, targetId, "DEPENDS_ON");

        given()
                .header("X-Tenant-Id", TENANT_A)
                .queryParam("direction", "outgoing")
                .when()
                .get("/api/v2/relations/" + sourceId)
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThan(0)))
                .body("[0].sourceId", equalTo(sourceId));
    }

    @Test
    @Order(12)
    @DisplayName("GET /api/v2/relations/{id} — respects direction filter")
    void getRelations_respectsDirection() {
        String sourceId = createMemory(TENANT_A, "Direction Test Source",
                "Source for direction test.", "FACT", "INTERNAL");
        String targetId = createMemory(TENANT_A, "Direction Test Target",
                "Target for direction test.", "FACT", "INTERNAL");

        String relId = createRelation(TENANT_A, sourceId, targetId, "SUPPORTS");

        // Incoming from target's perspective should include this relation
        given()
                .header("X-Tenant-Id", TENANT_A)
                .queryParam("direction", "incoming")
                .when()
                .get("/api/v2/relations/" + targetId)
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThan(0)));
    }

    @Test
    @Order(13)
    @DisplayName("DELETE /api/v2/relations/{id} — returns 204 on successful delete")
    void deleteRelation_returns204() {
        String sourceId = createMemory(TENANT_A, "Delete Relation Source",
                "Source for delete test.", "FACT", "INTERNAL");
        String targetId = createMemory(TENANT_A, "Delete Relation Target",
                "Target for delete test.", "FACT", "INTERNAL");

        String relId = createRelation(TENANT_A, sourceId, targetId, "RELATED_TO");

        given()
                .header("X-Tenant-Id", TENANT_A)
                .when()
                .delete("/api/v2/relations/" + relId)
                .then()
                .statusCode(204);
    }

    @Test
    @Order(14)
    @DisplayName("DELETE /api/v2/relations/{id} — returns 404 for non-existent relation")
    void deleteRelation_nonExistent_returns404() {
        given()
                .header("X-Tenant-Id", TENANT_A)
                .when()
                .delete("/api/v2/relations/" + UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    // ── Admin Reindex Tests ──────────────────────────────────────────

    @Test
    @Order(15)
    @DisplayName("POST /api/v2/admin/reindex — returns 200 with admin role")
    void reindex_withAdminRole_returns200() {
        createMemory(TENANT_A, "Reindex-ready Memory",
                "Content ready for reindexing.", "FACT", "INTERNAL");

        given()
                .header("X-Tenant-Id", TENANT_A)
                .header("X-Role", "admin")
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v2/admin/reindex")
                .then()
                .statusCode(200)
                .body("status", equalTo("completed"))
                .body("indexedFragments", greaterThan(0));
    }

    @Test
    @Order(16)
    @DisplayName("POST /api/v2/admin/reindex — returns 403 without admin role")
    void reindex_withoutAdminRole_returns403() {
        given()
                .header("X-Tenant-Id", TENANT_A)
                .header("X-Role", "user")
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v2/admin/reindex")
                .then()
                .statusCode(403);
    }

    // ── Tenant Isolation Tests ───────────────────────────────────────

    @Test
    @Order(17)
    @DisplayName("POST /api/v2/search/semantic — respects tenant isolation")
    void semanticSearch_respectsTenantIsolation() {
        String id = createMemory(TENANT_A, "TenantA-Only Search",
                "Only visible to tenant A.", "FACT", "INTERNAL");

        // Tenant B searching should not see Tenant A's data
        var items = given()
                .header("X-Tenant-Id", TENANT_B)
                .contentType(ContentType.JSON)
                .body(Map.of("query", "visible"))
                .when()
                .post("/api/v2/search/semantic")
                .then()
                .statusCode(200)
                .extract()
                .path("items");

        if (items instanceof java.util.List<?> list && !list.isEmpty()) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    var tid = map.get("tenantId");
                    assert tid == null || !TENANT_A.equals(tid)
                            : "Tenant B should not see Tenant A data";
                }
            }
        }
    }

    // ── BUG-004: Relation endpoint regression tests ──────────────────

    @Test
    @Order(18)
    @DisplayName("POST /api/v2/relations — accepts 'type' alias for relationType (BUG-004)")
    void createRelation_typeAlias_returns201() {
        String sourceId = createMemory(TENANT_A, "Alias Source",
                "Source for alias test.", "FACT", "INTERNAL");
        String targetId = createMemory(TENANT_A, "Alias Target",
                "Target for alias test.", "FACT", "INTERNAL");

        given()
                .header("X-Tenant-Id", TENANT_A)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "sourceId", sourceId,
                        "targetId", targetId,
                        "type", "RELATED_TO"
                ))
                .when()
                .post("/api/v2/relations")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("sourceId", equalTo(sourceId))
                .body("targetId", equalTo(targetId))
                .body("relationType", equalTo("RELATED_TO"));
    }

    @Test
    @Order(19)
    @DisplayName("POST /api/v2/relations — invalid RelationType returns 400 not 500 (BUG-004)")
    void createRelation_invalidType_returns400() {
        String sourceId = createMemory(TENANT_A, "InvalidType Source",
                "Source for invalid type test.", "FACT", "INTERNAL");
        String targetId = createMemory(TENANT_A, "InvalidType Target",
                "Target for invalid type test.", "FACT", "INTERNAL");

        given()
                .header("X-Tenant-Id", TENANT_A)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "sourceId", sourceId,
                        "targetId", targetId,
                        "relationType", "NONEXISTENT_TYPE"
                ))
                .when()
                .post("/api/v2/relations")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("INVALID_REQUEST"))
                .body("message", containsString("RelationType"));
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private String createMemory(String tenantId, String title, String content, String kind, String sensitivity) {
        return given()
                .header("X-Tenant-Id", tenantId)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "title", title,
                        "content", content,
                        "kind", kind,
                        "sensitivityLevel", sensitivity
                ))
                .when()
                .post("/api/v2/memories")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    private String createRelation(String tenantId, String sourceId, String targetId, String type) {
        return given()
                .header("X-Tenant-Id", tenantId)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "sourceId", sourceId,
                        "targetId", targetId,
                        "relationType", type
                ))
                .when()
                .post("/api/v2/relations")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }
}

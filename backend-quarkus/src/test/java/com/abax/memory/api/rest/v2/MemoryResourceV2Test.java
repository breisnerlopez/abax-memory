package com.abax.memory.api.rest.v2;

import com.abax.memory.test.H2TestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.containsString;

/**
 * Integration tests for {@link MemoryResourceV2} — CRUD endpoints.
 *
 * <p>Uses H2 in-memory database (Hibernate drop-and-create)
 * and mock tenant authentication via {@code X-Tenant-Id} header.</p>
 *
 * <p>References: HU-004.1.1 through HU-004.5.1</p>
 */
@QuarkusTest
@TestProfile(H2TestProfile.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("MemoryResourceV2 — CRUD endpoints")
class MemoryResourceV2Test {

    private static final String TENANT_A = "tenant-a";
    private static final String TENANT_B = "tenant-b";
    private static final String BASE_PATH = "/api/v2/memories";

    private String createdMemoryId;

    @BeforeEach
    void logSeparator() {
        System.out.println("── test boundary ──");
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/v2/memories — creates a memory and returns 201")
    void createMemory_returns201() {
        createdMemoryId = given()
                .header("X-Tenant-Id", TENANT_A)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "title", "Runbook: Database Failover",
                        "content", "## Procedure\n1. Check replication lag.\n2. Promote standby.\n3. Notify team.",
                        "kind", "FACT",
                        "sensitivityLevel", "INTERNAL",
                        "sourceType", "manual",
                        "confidence", 0.85
                ))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201)
                .header("Location", containsString(BASE_PATH))
                .body("id", notNullValue())
                .body("title", equalTo("Runbook: Database Failover"))
                .body("content", containsString("Procedure"))
                .body("kind", equalTo("fact"))
                .body("lifecycleState", equalTo("draft"))
                .body("sensitivityLevel", equalTo("internal"))
                .body("confidence", equalTo(0.85f))
                .body("isDeleted", equalTo(false))
                .body("isConsumerVisible", equalTo(false))
                .body("tenantId", equalTo(TENANT_A))
                .body("createdAt", notNullValue())
                .body("updatedAt", notNullValue())
                .extract()
                .path("id");

        System.out.println("Created memory ID: " + createdMemoryId);
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/v2/memories/{id} — retrieves memory by ID")
    void getById_returns200() {
        // First create one to ensure we have an ID
        String id = given()
                .header("X-Tenant-Id", TENANT_A)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "title", "Incident: API Outage",
                        "content", "## Root cause\nMisconfigured load balancer."
                ))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        given()
                .header("X-Tenant-Id", TENANT_A)
                .when()
                .get(BASE_PATH + "/" + id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("title", equalTo("Incident: API Outage"))
                .body("tenantId", equalTo(TENANT_A));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/v2/memories/{id} — returns 404 for cross-tenant access")
    void getById_crossTenant_returns404() {
        // Create in tenant A
        String id = given()
                .header("X-Tenant-Id", TENANT_A)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "title", "Secret Decision A",
                        "content", "Only for tenant A."
                ))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        // Attempt access from tenant B → 404 (not 403, to hide existence)
        given()
                .header("X-Tenant-Id", TENANT_B)
                .when()
                .get(BASE_PATH + "/" + id)
                .then()
                .statusCode(404)
                .body("errorCode", equalTo("NOT_FOUND"));
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/v2/memories/{id} — returns 404 for non-existent ID")
    void getById_nonExistent_returns404() {
        given()
                .header("X-Tenant-Id", TENANT_A)
                .when()
                .get(BASE_PATH + "/" + UUID.randomUUID())
                .then()
                .statusCode(404)
                .body("errorCode", equalTo("NOT_FOUND"));
    }

    @Test
    @Order(5)
    @DisplayName("PUT /api/v2/memories/{id} — updates title and lifecycle state")
    void updateMemory_returns200() {
        // Create in DRAFT
        String id = given()
                .header("X-Tenant-Id", TENANT_A)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "title", "Original Title",
                        "content", "Original content for update test."
                ))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        // Update
        given()
                .header("X-Tenant-Id", TENANT_A)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "title", "Updated Title",
                        "summary", "New summary after review.",
                        "lifecycleState", "PENDING"
                ))
                .when()
                .put(BASE_PATH + "/" + id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("title", equalTo("Updated Title"))
                .body("summary", equalTo("New summary after review."))
                .body("lifecycleState", equalTo("pending"))
                .body("content", equalTo("Original content for update test."));
    }

    @Test
    @Order(6)
    @DisplayName("PUT /api/v2/memories/{id} — rejects invalid lifecycle transition")
    void updateMemory_invalidTransition_returns400() {
        // Create and move to PENDING then try invalid transition
        String id = given()
                .header("X-Tenant-Id", TENANT_A)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "title", "Transition Test",
                        "content", "Testing lifecycle transitions."
                ))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        // Move DRAFT → PENDING (valid)
        given()
                .header("X-Tenant-Id", TENANT_A)
                .contentType(ContentType.JSON)
                .body(Map.of("lifecycleState", "PENDING"))
                .when()
                .put(BASE_PATH + "/" + id)
                .then()
                .statusCode(200);

        // Try PENDING → ARCHIVED (invalid: PENDING → ARCHIVED is not allowed)
        given()
                .header("X-Tenant-Id", TENANT_A)
                .contentType(ContentType.JSON)
                .body(Map.of("lifecycleState", "ARCHIVED"))
                .when()
                .put(BASE_PATH + "/" + id)
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("INVALID_REQUEST"))
                .body("message", containsString("lifecycle transition"));
    }

    @Test
    @Order(7)
    @DisplayName("DELETE /api/v2/memories/{id} — soft-deletes and returns 204")
    void softDelete_returns204() {
        String id = given()
                .header("X-Tenant-Id", TENANT_A)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "title", "To Be Deleted",
                        "content", "Temporary memory."
                ))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        // Soft-delete
        given()
                .header("X-Tenant-Id", TENANT_A)
                .when()
                .delete(BASE_PATH + "/" + id)
                .then()
                .statusCode(204);

        // Verify deleted resource is hidden (404)
        given()
                .header("X-Tenant-Id", TENANT_A)
                .when()
                .get(BASE_PATH + "/" + id)
                .then()
                .statusCode(404);
    }

    @Test
    @Order(8)
    @DisplayName("DELETE /api/v2/memories/{id} — idempotent on already deleted")
    void softDelete_idempotent_returns204() {
        String id = given()
                .header("X-Tenant-Id", TENANT_A)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "title", "Idempotent Delete Test",
                        "content", "Will be deleted twice."
                ))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        // First delete
        given()
                .header("X-Tenant-Id", TENANT_A)
                .when()
                .delete(BASE_PATH + "/" + id)
                .then()
                .statusCode(204);

        // Second delete (idempotent)
        given()
                .header("X-Tenant-Id", TENANT_A)
                .when()
                .delete(BASE_PATH + "/" + id)
                .then()
                .statusCode(204);
    }

    @Test
    @Order(9)
    @DisplayName("DELETE /api/v2/memories/{id} — cross-tenant returns 404")
    void softDelete_crossTenant_returns404() {
        String id = given()
                .header("X-Tenant-Id", TENANT_A)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "title", "A-Only Resource",
                        "content", "Only tenant A can delete this."
                ))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        given()
                .header("X-Tenant-Id", TENANT_B)
                .when()
                .delete(BASE_PATH + "/" + id)
                .then()
                .statusCode(404);
    }

    @Test
    @Order(10)
    @DisplayName("GET /api/v2/memories — list with filters, pagination, and facets")
    void list_returnsPaginatedResults() {
        // Create several memories to populate the index
        for (int i = 0; i < 3; i++) {
            given()
                    .header("X-Tenant-Id", TENANT_A)
                    .contentType(ContentType.JSON)
                    .body(Map.of(
                            "title", "Memory " + i,
                            "content", "Content for memory " + i,
                            "kind", "FACT"
                    ))
                    .when()
                    .post(BASE_PATH)
                    .then()
                    .statusCode(201);
        }

        // List without any query filter (query="*")
        given()
                .header("X-Tenant-Id", TENANT_A)
                .queryParam("query", "*")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .body("items", hasSize(greaterThan(0)))
                .body("total", greaterThan(0))
                .body("page", equalTo(0))
                .body("facets.kind", notNullValue())
                .body("facets.lifecycleState", notNullValue())
                .body("facets.sensitivityLevel", notNullValue());
    }

    @Test
    @Order(11)
    @DisplayName("GET /api/v2/memories — filter by kind")
    void list_filterByKind_returnsFilteredResults() {
        // Create one DECISION memory
        given()
                .header("X-Tenant-Id", TENANT_A)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "title", "Architecture Decision",
                        "content", "We chose PostgreSQL.",
                        "kind", "DECISION"
                ))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201);

        // List with kind filter
        given()
                .header("X-Tenant-Id", TENANT_A)
                .queryParam("query", "*")
                .queryParam("kind", "DECISION")
                .queryParam("size", 20)
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .body("items.size()", greaterThan(0))
                .body("items[0].kind", equalTo("decision"));
    }

    @Test
    @Order(12)
    @DisplayName("GET /api/v2/memories — tenant isolation in listing")
    void list_tenantIsolation_returnsOnlyOwnData() {
        // Create in tenant A
        given()
                .header("X-Tenant-Id", TENANT_A)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "title", "A-Only Visibility Test",
                        "content", "Tenant A's private memory."
                ))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201);

        // List from tenant B should not see A's data
        var items = given()
                .header("X-Tenant-Id", TENANT_B)
                .queryParam("query", "Visibility")
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .extract()
                .path("items");

        // Tenant B should not see Tenant A's data
        if (items instanceof java.util.List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    var tenantId = map.get("tenantId");
                    assert tenantId == null || !TENANT_A.equals(tenantId)
                            : "Tenant B should not see Tenant A's data";
                }
            }
        }
    }

    @Test
    @Order(13)
    @DisplayName("POST /api/v2/memories — validation error when title is blank")
    void createMemory_blankTitle_returns400() {
        given()
                .header("X-Tenant-Id", TENANT_A)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "title", "  ",
                        "content", "Some content"
                ))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(400);
    }

    @Test
    @Order(14)
    @DisplayName("POST /api/v2/memories — validation error when content is missing")
    void createMemory_missingContent_returns400() {
        given()
                .header("X-Tenant-Id", TENANT_A)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "title", "Missing Content"
                ))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(400);
    }

    @Test
    @Order(15)
    @DisplayName("POST /api/v2/memories — invalid MemoryKind returns 400 not 500 (BUG-013)")
    void createMemory_invalidKind_returns400() {
        given()
                .header("X-Tenant-Id", TENANT_A)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "title", "Invalid Kind Test",
                        "content", "Testing invalid kind handling.",
                        "kind", "INVALID_KIND",
                        "sensitivityLevel", "PUBLIC"
                ))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("INVALID_REQUEST"))
                .body("message", containsString("MemoryKind"));
    }

    @Test
    @Order(16)
    @DisplayName("PUT /api/v2/memories/{id} — invalid LifecycleState returns 400 not 500 (BUG-014)")
    void updateMemory_invalidLifecycleState_returns400() {
        // Create a memory first
        String id = given()
                .header("X-Tenant-Id", TENANT_A)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "title", "Lifecycle State Test",
                        "content", "Testing invalid lifecycle state handling."
                ))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        // Attempt update with an invalid lifecycle state value
        given()
                .header("X-Tenant-Id", TENANT_A)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "title", "Updated Title",
                        "lifecycleState", "INVALID_STATE"
                ))
                .when()
                .put(BASE_PATH + "/" + id)
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("INVALID_REQUEST"))
                .body("message", containsString("LifecycleState"));
    }

    @Test
    @Order(17)
    @DisplayName("POST /api/v2/memories — invalid SensitivityLevel returns 400 not 500")
    void createMemory_invalidSensitivity_returns400() {
        given()
                .header("X-Tenant-Id", TENANT_A)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "title", "Invalid Sensitivity Test",
                        "content", "Testing invalid sensitivity handling.",
                        "sensitivityLevel", "TOP_SECRET"
                ))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("INVALID_REQUEST"))
                .body("message", containsString("SensitivityLevel"));
    }
}

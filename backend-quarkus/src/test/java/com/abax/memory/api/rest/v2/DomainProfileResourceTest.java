package com.abax.memory.api.rest.v2;

import com.abax.memory.api.dto.v2.DomainProfileRequest;
import com.abax.memory.test.H2TestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * REST-API tests for {@link DomainProfileResource} — CP-V21-041+.
 *
 * <p>Tests CRUD operations on the /api/v2/domains endpoints,
 * including admin role enforcement for DELETE. Each test is
 * self-contained due to {@code @Transactional} rollback per test.</p>
 *
 * <p>New in v2.1.0 — CP-V21-041+ (Gap 2: Domain profile management API).</p>
 *
 * <p>References: CP-V21-041+, HU-002.1, HU-002.2</p>
 */
@QuarkusTest
@TestProfile(H2TestProfile.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("DomainProfileResource — CRUD REST-API tests")
class DomainProfileResourceTest {

    private static final String TEST_TENANT = "test-domain-tenant";

    // ── Helper ───────────────────────────────────────────────────────

    private Map<String, Object> sampleConfig() {
        return Map.of(
                "recommendedKinds", java.util.List.of("EVENT", "PROCEDURE"),
                "defaultSensitivity", "INTERNAL",
                "defaultConfidence", 0.7
        );
    }

    /**
     * Seeds a profile via API for tests that need an existing profile.
     */
    private void seedProfile(String name, String description) {
        given()
                .header("X-Tenant-Id", TEST_TENANT)
                .contentType(ContentType.JSON)
                .body(new DomainProfileRequest(sampleConfig(), description, "1.0", true))
                .when().put("/api/v2/domains/" + name)
                .then()
                .statusCode(200);
    }

    // ── GET /api/v2/domains — List profiles ──────────────────────────

    @Test
    @Order(1)
    @DisplayName("GET /api/v2/domains — returns results for a tenant")
    @Transactional
    void listProfiles_shouldReturnResults() {
        // Seed a profile first
        seedProfile("list-test", "Listing test profile");

        var config = sampleConfig();
        given()
                .header("X-Tenant-Id", TEST_TENANT)
                .when().get("/api/v2/domains")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }

    // ── PUT /api/v2/domains/{name} — Create profile ──────────────────

    @Test
    @Order(2)
    @DisplayName("PUT /api/v2/domains/{name} — creates a new domain profile")
    @Transactional
    void createProfile_shouldCreateNewProfile() {
        var config = sampleConfig();

        given()
                .header("X-Tenant-Id", TEST_TENANT)
                .contentType(ContentType.JSON)
                .body(new DomainProfileRequest(config, "Test profile for v2.1.0", "1.0", true))
                .when().put("/api/v2/domains/create-test-profile")
                .then()
                .statusCode(200)
                .header("X-Created", "true")
                .body("name", equalTo("create-test-profile"))
                .body("version", equalTo("1.0"))
                .body("description", equalTo("Test profile for v2.1.0"))
                .body("active", equalTo(true))
                .body("id", notNullValue())
                .body("createdAt", notNullValue())
                .body("updatedAt", notNullValue());
    }

    // ── GET /api/v2/domains/{name} — Get single profile ──────────────

    @Test
    @Order(3)
    @DisplayName("GET /api/v2/domains/{name} — retrieves an existing profile")
    @Transactional
    void getProfileByName_shouldReturnExistingProfile() {
        // Create a profile within the same test (self-contained)
        seedProfile("get-by-name-test", "Profile for get-by-name test");

        given()
                .header("X-Tenant-Id", TEST_TENANT)
                .when().get("/api/v2/domains/get-by-name-test")
                .then()
                .statusCode(200)
                .body("name", equalTo("get-by-name-test"))
                .body("description", equalTo("Profile for get-by-name test"))
                .body("id", notNullValue());
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/v2/domains/{name} — returns 404 for non-existent profile")
    @Transactional
    void getProfileByName_shouldReturn404ForNonExistent() {
        given()
                .header("X-Tenant-Id", TEST_TENANT)
                .when().get("/api/v2/domains/non-existent-profile")
                .then()
                .statusCode(404);
    }

    // ── PUT /api/v2/domains/{name} — Update profile ──────────────────

    @Test
    @Order(5)
    @DisplayName("PUT /api/v2/domains/{name} — updates an existing profile (upsert)")
    @Transactional
    void updateProfile_shouldUpdateExistingProfile() {
        // Create first, then update in the same test
        seedProfile("upsert-test", "Initial description");

        var updatedConfig = Map.of(
                "recommendedKinds", java.util.List.of("DECISION", "FACT"),
                "defaultSensitivity", "CONFIDENTIAL",
                "defaultConfidence", 0.85
        );

        given()
                .header("X-Tenant-Id", TEST_TENANT)
                .contentType(ContentType.JSON)
                .body(new DomainProfileRequest(updatedConfig, "Updated description", "1.1", false))
                .when().put("/api/v2/domains/upsert-test")
                .then()
                .statusCode(200)
                .body("name", equalTo("upsert-test"))
                .body("version", equalTo("1.1"))
                .body("description", equalTo("Updated description"))
                .body("active", equalTo(false));
    }

    // ── PUT /api/v2/domains/{name} — Validation ──────────────────────

    @Test
    @Order(6)
    @DisplayName("PUT /api/v2/domains/{name} — rejects request without config")
    @Transactional
    void createProfile_shouldRejectWithoutConfig() {
        given()
                .header("X-Tenant-Id", TEST_TENANT)
                .contentType(ContentType.JSON)
                .body(Map.of("description", "Missing config"))
                .when().put("/api/v2/domains/missing-config-profile")
                .then()
                .statusCode(400);
    }

    // ── DELETE /api/v2/domains/{name} — Admin-only delete ────────────

    @Test
    @Order(7)
    @DisplayName("DELETE /api/v2/domains/{name} — rejects without admin role")
    @Transactional
    void deleteProfile_shouldRejectWithoutAdminRole() {
        seedProfile("reject-delete-test", "Should not be deletable by user role");

        given()
                .header("X-Tenant-Id", TEST_TENANT)
                .header("X-Role", "user")
                .when().delete("/api/v2/domains/reject-delete-test")
                .then()
                .statusCode(403)
                .body("errorCode", equalTo("FORBIDDEN"));
    }

    @Test
    @Order(8)
    @DisplayName("DELETE /api/v2/domains/{name} — deletes with admin role")
    @Transactional
    void deleteProfile_shouldDeleteWithAdminRole() {
        String deleteProfileName = "admin-delete-me";
        seedProfile(deleteProfileName, "Profile to delete by admin");

        // Delete with admin role
        given()
                .header("X-Tenant-Id", TEST_TENANT)
                .header("X-Role", "admin")
                .when().delete("/api/v2/domains/" + deleteProfileName)
                .then()
                .statusCode(204);

        // Verify it's gone
        given()
                .header("X-Tenant-Id", TEST_TENANT)
                .when().get("/api/v2/domains/" + deleteProfileName)
                .then()
                .statusCode(404);
    }

    @Test
    @Order(9)
    @DisplayName("DELETE /api/v2/domains/{name} — returns 404 for non-existent profile")
    @Transactional
    void deleteProfile_shouldReturn404ForNonExistent() {
        given()
                .header("X-Tenant-Id", TEST_TENANT)
                .header("X-Role", "admin")
                .when().delete("/api/v2/domains/non-existent-profile")
                .then()
                .statusCode(404);
    }
}

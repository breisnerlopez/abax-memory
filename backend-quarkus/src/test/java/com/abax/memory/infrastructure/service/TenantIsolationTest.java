package com.abax.memory.infrastructure.service;

import com.abax.memory.api.dto.v2.CreateMemoryRequest;
import com.abax.memory.api.dto.v2.MemoryResponse;
import com.abax.memory.api.dto.v2.SearchRequest;
import com.abax.memory.domain.enums.LifecycleState;
import com.abax.memory.domain.enums.MemoryKind;
import com.abax.memory.domain.enums.SensitivityLevel;
import com.abax.memory.domain.service.MemoryService;
import com.abax.memory.test.H2TestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tenant isolation tests for EP-003: Scoping Multi-Tenant.
 *
 * <p>Verifies that:
 * <ul>
 *   <li>Two different tenants cannot see each other's data.</li>
 *   <li>A tenant cannot access a memory of another tenant (404).</li>
 *   <li>Missing X-Tenant-Id header results in 401 (tested at filter level).</li>
 * </ul>
 *
 * <p>References: HU-003.1, HU-003.2, SC-03, SC-04</p>
 */
@QuarkusTest
@TestProfile(H2TestProfile.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("EP-003: Tenant Isolation Tests")
class TenantIsolationTest {

    private static final String TENANT_ALPHA = "tenant-alpha";
    private static final String TENANT_BETA = "tenant-beta";

    @Inject
    MemoryService memoryService;

    // ── D1.1: Two tenants cannot see each other's data ───────────────

    @Test
    @Order(1)
    @DisplayName("D1.1 — Tenants are isolated: Tenant B cannot see Tenant A's memories")
    @Transactional
    void twoTenants_shouldNotSeeEachOthersData() {
        // Tenant Alpha creates memories
        var alphaRequest = new CreateMemoryRequest(
                "Alpha Memory 1", "Content from alpha tenant.",
                MemoryKind.DECISION, "scope-alpha", SensitivityLevel.INTERNAL,
                null, null, 0.9, null);
        memoryService.createV2(alphaRequest, TENANT_ALPHA);

        var alphaRequest2 = new CreateMemoryRequest(
                "Alpha Memory 2", "More alpha content.",
                MemoryKind.FACT, "scope-alpha", null, null, null, null, null);
        memoryService.createV2(alphaRequest2, TENANT_ALPHA);

        // Tenant Beta creates memories
        var betaRequest = new CreateMemoryRequest(
                "Beta Memory 1", "Content from beta tenant.",
                MemoryKind.PROCEDURE, "scope-beta", SensitivityLevel.CONFIDENTIAL,
                null, null, 0.7, null);
        memoryService.createV2(betaRequest, TENANT_BETA);

        // Tenant Beta searches — should only see Beta's data
        var betaSearch = new SearchRequest("*", null, null, null, null, null, null, 0, 20);
        var betaResults = memoryService.listV2(betaSearch, TENANT_BETA);

        assertThat(betaResults.items()).hasSize(1);
        assertThat(betaResults.items().get(0).tenantId()).isEqualTo(TENANT_BETA);
        assertThat(betaResults.items().get(0).title()).isEqualTo("Beta Memory 1");

        // Tenant Alpha searches — should only see Alpha's data
        var alphaSearch = new SearchRequest("*", null, null, null, null, null, null, 0, 20);
        var alphaResults = memoryService.listV2(alphaSearch, TENANT_ALPHA);

        assertThat(alphaResults.items()).hasSize(2);
        alphaResults.items().forEach(item ->
                assertThat(item.tenantId()).isEqualTo(TENANT_ALPHA));
    }

    // ── D1.2: Cross-tenant access returns 404 ───────────────────────

    @Test
    @Order(2)
    @DisplayName("D1.2 — Cross-tenant access returns 404 (does not reveal existence)")
    @Transactional
    void crossTenantAccess_shouldReturn404() {
        // Tenant Alpha creates a memory
        var request = new CreateMemoryRequest(
                "Private Alpha Memory", "Secret alpha content.",
                MemoryKind.ENTITY, null, SensitivityLevel.CONFIDENTIAL,
                null, null, null, null);
        MemoryResponse created = memoryService.createV2(request, TENANT_ALPHA);

        // Tenant Beta tries to access it — should get 404
        assertThatThrownBy(() -> memoryService.getByIdV2(created.id(), TENANT_BETA))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("not found");

        // Tenant Beta tries to update it — should get 404
        var updateRequest = new com.abax.memory.api.dto.v2.UpdateMemoryRequest(
                "Hacked Title", null, null, null, null, null);
        assertThatThrownBy(() -> memoryService.updateV2(created.id(), updateRequest, TENANT_BETA))
                .isInstanceOf(NotFoundException.class);

        // Tenant Beta tries to soft-delete it — should get 404
        assertThatThrownBy(() -> memoryService.softDeleteV2(created.id(), TENANT_BETA))
                .isInstanceOf(NotFoundException.class);
    }

    // ── D1.3: Scope validation prevents cross-scope misuse ──────────

    @Test
    @Order(3)
    @DisplayName("D1.3 — Scope validation: scope from another tenant cannot be reused")
    @Transactional
    void scopeValidation_preventsCrossScopeReuse() {
        // Tenant Alpha uses scope "project-phoenix"
        var alphaRequest = new CreateMemoryRequest(
                "Phoenix Project Memo", "Project details for Phoenix.",
                MemoryKind.DECISION, "project-phoenix", null, null, null, null, null);
        memoryService.createV2(alphaRequest, TENANT_ALPHA);

        // Tenant Beta tries to use the same scope — should fail
        var betaRequest = new CreateMemoryRequest(
                "Beta Phoenix Claim", "Trying to claim Phoenix.",
                MemoryKind.PROCEDURE, "project-phoenix", null, null, null, null, null);
        assertThatThrownBy(() -> memoryService.createV2(betaRequest, TENANT_BETA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to tenant");
    }

    // ── D1.4: List respects tenant isolation ────────────────────────

    @Test
    @Order(4)
    @DisplayName("D1.4 — listV2 returns empty for tenant with no data")
    @Transactional
    void listV2_returnsEmptyForUnknownTenant() {
        var search = new SearchRequest("*", null, null, null, null, null, null, 0, 20);
        var results = memoryService.listV2(search, "non-existent-tenant");

        assertThat(results.items()).isEmpty();
        assertThat(results.total()).isEqualTo(0);
    }

    // ── D1.5: Cross-tenant through review workflow ──────────────────

    @Test
    @Order(5)
    @DisplayName("D1.5 — Review workflow is tenant-scoped")
    @Transactional
    void reviewWorkflow_isTenantScoped() {
        // Tenant Alpha creates memory and submits for review
        var request = new CreateMemoryRequest(
                "Alpha Review Target", "Needs review.",
                MemoryKind.DECISION, null, null, null, null, null, null);
        MemoryResponse created = memoryService.createV2(request, TENANT_ALPHA);

        // Submit for review as Alpha
        memoryService.requestReview(created.id(), TENANT_ALPHA, "reviewer-alpha");

        // Tenant Beta tries to approve — should get 404
        assertThatThrownBy(() ->
                memoryService.approveReview(created.id(), TENANT_BETA, "reviewer-beta", "Looks good"))
                .isInstanceOf(NotFoundException.class);
    }
}

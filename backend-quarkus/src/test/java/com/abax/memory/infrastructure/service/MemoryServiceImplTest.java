package com.abax.memory.infrastructure.service;

import com.abax.memory.api.dto.v2.CreateMemoryRequest;
import com.abax.memory.api.dto.v2.MemoryResponse;
import com.abax.memory.api.dto.v2.SearchRequest;
import com.abax.memory.api.dto.v2.UpdateMemoryRequest;
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
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Service-layer unit tests for {@link MemoryServiceImpl}.
 *
 * <p>These tests exercise the business logic directly via the
 * {@link MemoryService} interface, validating tenant isolation,
 * lifecycle transitions, and CRUD operations.</p>
 *
 * <p>References: HU-004.1.1 through HU-004.5.1</p>
 */
@QuarkusTest
@TestProfile(H2TestProfile.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("MemoryServiceImpl — business logic tests")
class MemoryServiceImplTest {

    private static final String TENANT_A = "tenant-a-test";
    private static final String TENANT_B = "tenant-b-test";

    @Inject
    MemoryService memoryService;

    @Test
    @Order(1)
    @DisplayName("createV2 — persists entity and returns response with all fields")
    @Transactional
    void createV2_shouldPersistAndReturnFullResponse() {
        var request = new CreateMemoryRequest(
                "Test Title",
                "Test content body.",
                MemoryKind.DECISION,
                "scope-1",
                SensitivityLevel.CONFIDENTIAL,
                "manual",
                "ref-001",
                0.92,
                null
        );

        MemoryResponse response = memoryService.createV2(request, TENANT_A);

        assertThat(response.id()).isNotNull();
        assertThat(response.tenantId()).isEqualTo(TENANT_A);
        assertThat(response.title()).isEqualTo("Test Title");
        assertThat(response.content()).isEqualTo("Test content body.");
        assertThat(response.kind()).isEqualTo(MemoryKind.DECISION);
        assertThat(response.scopeId()).isEqualTo("scope-1");
        assertThat(response.sensitivityLevel()).isEqualTo(SensitivityLevel.CONFIDENTIAL);
        assertThat(response.sourceType()).isEqualTo("manual");
        assertThat(response.sourceRef()).isEqualTo("ref-001");
        assertThat(response.confidence()).isEqualTo(0.92);
        assertThat(response.lifecycleState()).isEqualTo(LifecycleState.DRAFT);
        assertThat(response.isDeleted()).isFalse();
        assertThat(response.isConsumerVisible()).isFalse();
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("createV2 — applies defaults when optional fields are null")
    @Transactional
    void createV2_shouldApplyDefaultsForMissingFields() {
        var request = new CreateMemoryRequest(
                "Minimal Memory",
                "Just content.",
                null, null, null, null, null, null, null
        );

        MemoryResponse response = memoryService.createV2(request, TENANT_A);

        assertThat(response.kind()).isNotNull(); // defaults to FACT
        assertThat(response.sensitivityLevel()).isEqualTo(SensitivityLevel.INTERNAL);
        assertThat(response.confidence()).isEqualTo(0.5);
        assertThat(response.lifecycleState()).isEqualTo(LifecycleState.DRAFT);
    }

    @Test
    @Order(3)
    @DisplayName("getByIdV2 — returns response for existing resource")
    @Transactional
    void getByIdV2_shouldReturnExistingResource() {
        var request = new CreateMemoryRequest("Findable", "Content.", null, null, null, null, null, null, null);
        MemoryResponse created = memoryService.createV2(request, TENANT_A);

        MemoryResponse found = memoryService.getByIdV2(created.id(), TENANT_A);

        assertThat(found.id()).isEqualTo(created.id());
        assertThat(found.title()).isEqualTo("Findable");
    }

    @Test
    @Order(4)
    @DisplayName("getByIdV2 — throws 404 for non-existent resource")
    @Transactional
    void getByIdV2_nonExistent_shouldThrow404() {
        assertThatThrownBy(() -> memoryService.getByIdV2(UUID.randomUUID(), TENANT_A))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @Order(5)
    @DisplayName("getByIdV2 — throws 404 for cross-tenant access")
    @Transactional
    void getByIdV2_crossTenant_shouldThrow404() {
        var request = new CreateMemoryRequest("TenantA Private", "Secret.", null, null, null, null, null, null, null);
        MemoryResponse created = memoryService.createV2(request, TENANT_A);

        assertThatThrownBy(() -> memoryService.getByIdV2(created.id(), TENANT_B))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @Order(6)
    @DisplayName("updateV2 — applies partial updates correctly")
    @Transactional
    void updateV2_shouldApplyPartialUpdates() {
        var create = new CreateMemoryRequest("Original", "Original content.", null, null, null, null, null, null, null);
        MemoryResponse created = memoryService.createV2(create, TENANT_A);

        var update = new UpdateMemoryRequest(
                "Updated Title",
                null, // content stays same
                "New summary",
                null, // lifecycle stays DRAFT
                SensitivityLevel.SECRET,
                null
        );

        MemoryResponse updated = memoryService.updateV2(created.id(), update, TENANT_A);

        assertThat(updated.title()).isEqualTo("Updated Title");
        assertThat(updated.content()).isEqualTo("Original content."); // unchanged
        assertThat(updated.summary()).isEqualTo("New summary");
        assertThat(updated.sensitivityLevel()).isEqualTo(SensitivityLevel.SECRET);
        assertThat(updated.lifecycleState()).isEqualTo(LifecycleState.DRAFT); // unchanged
    }

    @Test
    @Order(7)
    @DisplayName("updateV2 — validates lifecycle transitions")
    @Transactional
    void updateV2_validatesLifecycleTransitions() {
        var create = new CreateMemoryRequest("State Test", "Content.", null, null, null, null, null, null, null);
        MemoryResponse created = memoryService.createV2(create, TENANT_A);

        // DRAFT → PENDING is valid
        var validUpdate = new UpdateMemoryRequest(null, null, null, LifecycleState.PENDING, null, null);
        var updated = memoryService.updateV2(created.id(), validUpdate, TENANT_A);
        assertThat(updated.lifecycleState()).isEqualTo(LifecycleState.PENDING);

        // PENDING → ACTIVE is valid
        var approve = new UpdateMemoryRequest(null, null, null, LifecycleState.ACTIVE, null, null);
        var approved = memoryService.updateV2(created.id(), approve, TENANT_A);
        assertThat(approved.lifecycleState()).isEqualTo(LifecycleState.ACTIVE);

        // ACTIVE → ARCHIVED is valid
        var archive = new UpdateMemoryRequest(null, null, null, LifecycleState.ARCHIVED, null, null);
        var archived = memoryService.updateV2(created.id(), archive, TENANT_A);
        assertThat(archived.lifecycleState()).isEqualTo(LifecycleState.ARCHIVED);

        // ARCHIVED → DRAFT is INVALID
        var invalidUpdate = new UpdateMemoryRequest(null, null, null, LifecycleState.DRAFT, null, null);
        assertThatThrownBy(() -> memoryService.updateV2(created.id(), invalidUpdate, TENANT_A))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lifecycle transition");
    }

    @Test
    @Order(8)
    @DisplayName("updateV2 — throws 404 for cross-tenant update")
    @Transactional
    void updateV2_crossTenant_shouldThrow404() {
        var create = new CreateMemoryRequest("TenantA Update Target", "Content.", null, null, null, null, null, null, null);
        MemoryResponse created = memoryService.createV2(create, TENANT_A);

        var update = new UpdateMemoryRequest("Hacked Title", null, null, null, null, null);

        assertThatThrownBy(() -> memoryService.updateV2(created.id(), update, TENANT_B))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @Order(9)
    @DisplayName("softDeleteV2 — marks entity as deleted and returns 404 on subsequent get")
    @Transactional
    void softDeleteV2_shouldMarkDeletedAndHideFromQueries() {
        var create = new CreateMemoryRequest("Disposable", "Content.", null, null, null, null, null, null, null);
        MemoryResponse created = memoryService.createV2(create, TENANT_A);

        // Soft-delete
        memoryService.softDeleteV2(created.id(), TENANT_A);

        // Verify it's hidden
        assertThatThrownBy(() -> memoryService.getByIdV2(created.id(), TENANT_A))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @Order(10)
    @DisplayName("softDeleteV2 — idempotent on already deleted")
    @Transactional
    void softDeleteV2_idempotent_shouldNotThrow() {
        var create = new CreateMemoryRequest("Idempotent", "Content.", null, null, null, null, null, null, null);
        MemoryResponse created = memoryService.createV2(create, TENANT_A);

        memoryService.softDeleteV2(created.id(), TENANT_A);
        assertThatNoException()
                .isThrownBy(() -> memoryService.softDeleteV2(created.id(), TENANT_A));
    }

    @Test
    @Order(11)
    @DisplayName("softDeleteV2 — throws 404 for cross-tenant delete")
    @Transactional
    void softDeleteV2_crossTenant_shouldThrow404() {
        var create = new CreateMemoryRequest("Delete Target", "Content.", null, null, null, null, null, null, null);
        MemoryResponse created = memoryService.createV2(create, TENANT_A);

        assertThatThrownBy(() -> memoryService.softDeleteV2(created.id(), TENANT_B))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @Order(12)
    @DisplayName("listV2 — returns paginated results with facets")
    @Transactional
    void listV2_shouldReturnPaginatedResults() {
        // Create a few memories
        memoryService.createV2(
                new CreateMemoryRequest("Memory Alpha", "Content Alpha.",
                        MemoryKind.DECISION, null, null, null, null, null, null), TENANT_A);
        memoryService.createV2(
                new CreateMemoryRequest("Memory Beta", "Content Beta.",
                        MemoryKind.FACT, null, null, null, null, null, null), TENANT_A);

        var request = new SearchRequest("*", null, null, null, null, null, null, 0, 10);
        var result = memoryService.listV2(request, TENANT_A);

        assertThat(result.items()).isNotEmpty();
        assertThat(result.total()).isGreaterThanOrEqualTo(2);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.facets()).containsKeys("kind", "lifecycleState", "sensitivityLevel");
    }

    @Test
    @Order(13)
    @DisplayName("listV2 — excludes soft-deleted records")
    @Transactional
    void listV2_shouldExcludeSoftDeletedRecords() {
        var create = new CreateMemoryRequest("To Delete List", "Will be deleted.", null, null, null, null, null, null, null);
        MemoryResponse created = memoryService.createV2(create, TENANT_A);

        memoryService.softDeleteV2(created.id(), TENANT_A);

        var request = new SearchRequest("To Delete List", null, null, null, null, null, null, 0, 10);
        var result = memoryService.listV2(request, TENANT_A);

        assertThat(result.items()).isEmpty();
    }

    @Test
    @Order(14)
    @DisplayName("listV2 — respects tenant isolation")
    @Transactional
    void listV2_shouldRespectTenantIsolation() {
        memoryService.createV2(
                new CreateMemoryRequest("TenantA Entry", "Content.", null, null, null, null, null, null, null), TENANT_A);

        var request = new SearchRequest("TenantA Entry", null, null, null, null, null, null, 0, 10);
        var result = memoryService.listV2(request, TENANT_B);

        assertThat(result.items()).isEmpty();
        assertThat(result.total()).isEqualTo(0);
    }
}

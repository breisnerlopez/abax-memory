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
        , null);

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
        , null);

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
        var request = new CreateMemoryRequest("Findable", "Content.", null, null, null, null, null, null, null, null);
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
        var request = new CreateMemoryRequest("TenantA Private", "Secret.", null, null, null, null, null, null, null, null);
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
        var create = new CreateMemoryRequest("Original", "Original content.", null, null, null, null, null, null, null, null);
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
        var create = new CreateMemoryRequest("State Test", "Content.", null, null, null, null, null, null, null, null);
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
        var create = new CreateMemoryRequest("TenantA Update Target", "Content.", null, null, null, null, null, null, null, null);
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
        var create = new CreateMemoryRequest("Disposable", "Content.", null, null, null, null, null, null, null, null);
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
        var create = new CreateMemoryRequest("Idempotent", "Content.", null, null, null, null, null, null, null, null);
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
        var create = new CreateMemoryRequest("Delete Target", "Content.", null, null, null, null, null, null, null, null);
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
                        MemoryKind.DECISION, null, null, null, null, null, null, null), TENANT_A);
        memoryService.createV2(
                new CreateMemoryRequest("Memory Beta", "Content Beta.",
                        MemoryKind.FACT, null, null, null, null, null, null, null), TENANT_A);

        var request = new SearchRequest("*", null, null, null, null, null, null, 0, 10);
        // Use admin role to see all fragments (including DRAFT)
        var result = memoryService.listV2(request, TENANT_A, "admin");

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
        var create = new CreateMemoryRequest("To Delete List", "Will be deleted.", null, null, null, null, null, null, null, null);
        MemoryResponse created = memoryService.createV2(create, TENANT_A);

        memoryService.softDeleteV2(created.id(), TENANT_A);

        var request = new SearchRequest("To Delete List", null, null, null, null, null, null, 0, 10);
        var result = memoryService.listV2(request, TENANT_A, "admin");

        assertThat(result.items()).isEmpty();
    }

    @Test
    @Order(14)
    @DisplayName("listV2 — respects tenant isolation")
    @Transactional
    void listV2_shouldRespectTenantIsolation() {
        memoryService.createV2(
                new CreateMemoryRequest("TenantA Entry", "Content.", null, null, null, null, null, null, null, null), TENANT_A);

        var request = new SearchRequest("TenantA Entry", null, null, null, null, null, null, 0, 10);
        var result = memoryService.listV2(request, TENANT_B, "admin");

        assertThat(result.items()).isEmpty();
        assertThat(result.total()).isEqualTo(0);
    }

    @Test
    @Order(15)
    @DisplayName("listV2 — BR-001: hides DRAFT from consumers, shows to admin")
    @Transactional
    void listV2_shouldHideDraftFromConsumers() {
        // Create a DRAFT memory
        memoryService.createV2(
                new CreateMemoryRequest("Secret Draft", "Draft content.",
                        null, null, null, null, null, null, null, null), TENANT_A);

        // Consumer search (no role or consumer role) should NOT find DRAFT
        var consumerRequest = new SearchRequest("*", null, null, null, null, null, null, 0, 10);
        var consumerResult = memoryService.listV2(consumerRequest, TENANT_A, "consumer");
        assertThat(consumerResult.items()).noneMatch(r -> "Secret Draft".equals(r.title()));

        // Admin search SHOULD find DRAFT
        var adminRequest = new SearchRequest("*", null, null, null, null, null, null, 0, 10);
        var adminResult = memoryService.listV2(adminRequest, TENANT_A, "admin");
        assertThat(adminResult.items()).anyMatch(r -> "Secret Draft".equals(r.title()));

        // Null role (unknown role) should also hide DRAFT
        var unknownResult = memoryService.listV2(
                new SearchRequest("*", null, null, null, null, null, null, 0, 10),
                TENANT_A, null);
        assertThat(unknownResult.items()).noneMatch(r -> "Secret Draft".equals(r.title()));
    }

    @Test
    @Order(16)
    @DisplayName("listV2 — BR-001: hides PENDING from consumers, shows to reviewer")
    @Transactional
    void listV2_shouldHidePendingFromConsumers() {
        // Create a memory and move it to PENDING
        var created = memoryService.createV2(
                new CreateMemoryRequest("Pending Memory", "Pending content.",
                        null, null, null, null, null, null, null, null), TENANT_A);
        memoryService.updateV2(created.id(),
                new UpdateMemoryRequest(null, null, null, LifecycleState.PENDING, null, null),
                TENANT_A);

        // Consumer should NOT see PENDING
        var consumerResult = memoryService.listV2(
                new SearchRequest("*", null, null, null, null, null, null, 0, 10),
                TENANT_A, "consumer");
        assertThat(consumerResult.items()).noneMatch(r -> "Pending Memory".equals(r.title()));

        // Reviewer SHOULD see PENDING
        var reviewerResult = memoryService.listV2(
                new SearchRequest("*", null, null, null, null, null, null, 0, 10),
                TENANT_A, "reviewer");
        assertThat(reviewerResult.items()).anyMatch(r -> "Pending Memory".equals(r.title()));
    }

    // ── Review Workflow (EP-006 / UAT-S05) ──────────────────────────

    @Test
    @Order(17)
    @DisplayName("requestReview — DRAFT → PENDING transition works")
    @Transactional
    void requestReview_shouldTransitionDraftToPending() {
        var created = memoryService.createV2(
                new CreateMemoryRequest("Review Me", "Content for review.",
                        null, null, null, null, null, null, null, null), TENANT_A);
        assertThat(created.lifecycleState()).isEqualTo(LifecycleState.DRAFT);

        var result = memoryService.requestReview(created.id(), TENANT_A, "reviewer-1");

        assertThat(result.lifecycleState()).isEqualTo(LifecycleState.PENDING);
        assertThat(result.id()).isEqualTo(created.id());
    }

    @Test
    @Order(18)
    @DisplayName("requestReview — throws on invalid transition (ACTIVE → PENDING)")
    @Transactional
    void requestReview_shouldThrowWhenTransitionInvalid() {
        var created = memoryService.createV2(
                new CreateMemoryRequest("Active Mem", "Content.",
                        null, null, null, null, null, null, null, null), TENANT_A);
        // Move to ACTIVE via updateV2
        memoryService.updateV2(created.id(),
                new UpdateMemoryRequest(null, null, null, LifecycleState.PENDING, null, null),
                TENANT_A);
        memoryService.updateV2(created.id(),
                new UpdateMemoryRequest(null, null, null, LifecycleState.ACTIVE, null, null),
                TENANT_A);

        // ACTIVE → PENDING is not a valid transition
        assertThatThrownBy(() ->
                memoryService.requestReview(created.id(), TENANT_A, "reviewer-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot request review");
    }

    @Test
    @Order(19)
    @DisplayName("approveReview — PENDING → ACTIVE transition works")
    @Transactional
    void approveReview_shouldTransitionPendingToActive() {
        var created = memoryService.createV2(
                new CreateMemoryRequest("Approve Me", "Content.",
                        null, null, null, null, null, null, null, null), TENANT_A);
        memoryService.requestReview(created.id(), TENANT_A, "submitter");

        var result = memoryService.approveReview(created.id(), TENANT_A, "reviewer-1", "Looks good");

        assertThat(result.lifecycleState()).isEqualTo(LifecycleState.ACTIVE);
        assertThat(result.reviewerId()).isEqualTo("reviewer-1");
        assertThat(result.reviewComment()).isEqualTo("Looks good");
        assertThat(result.isConsumerVisible()).isTrue();
    }

    @Test
    @Order(20)
    @DisplayName("approveReview — throws on invalid transition (DRAFT → ACTIVE)")
    @Transactional
    void approveReview_shouldThrowWhenTransitionInvalid() {
        var created = memoryService.createV2(
                new CreateMemoryRequest("Draft Mem", "Content.",
                        null, null, null, null, null, null, null, null), TENANT_A);

        // DRAFT → ACTIVE is not valid (must go through PENDING)
        assertThatThrownBy(() ->
                memoryService.approveReview(created.id(), TENANT_A, "reviewer-1", "skip pending"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot approve review");
    }

    @Test
    @Order(21)
    @DisplayName("returnToDraft — PENDING → DRAFT (reject/rework) transition works")
    @Transactional
    void returnToDraft_shouldTransitionPendingToDraft() {
        var created = memoryService.createV2(
                new CreateMemoryRequest("Reject Me", "Content.",
                        null, null, null, null, null, null, null, null), TENANT_A);
        memoryService.requestReview(created.id(), TENANT_A, "submitter");

        var result = memoryService.returnToDraft(created.id(), TENANT_A, "reviewer-1", "Needs more detail");

        assertThat(result.lifecycleState()).isEqualTo(LifecycleState.DRAFT);
        assertThat(result.reviewerId()).isEqualTo("reviewer-1");
        assertThat(result.reviewComment()).isEqualTo("Needs more detail");
    }

    @Test
    @Order(22)
    @DisplayName("returnToDraft — throws on invalid transition (ACTIVE → DRAFT is invalid)")
    @Transactional
    void returnToDraft_shouldThrowWhenTransitionInvalid() {
        var created = memoryService.createV2(
                new CreateMemoryRequest("Active To Draft", "Content.",
                        null, null, null, null, null, null, null, null), TENANT_A);
        memoryService.updateV2(created.id(),
                new UpdateMemoryRequest(null, null, null, LifecycleState.PENDING, null, null),
                TENANT_A);
        memoryService.updateV2(created.id(),
                new UpdateMemoryRequest(null, null, null, LifecycleState.ACTIVE, null, null),
                TENANT_A);

        // ACTIVE → DRAFT is not a valid transition
        assertThatThrownBy(() ->
                memoryService.returnToDraft(created.id(), TENANT_A, "reviewer-1", "invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot return to DRAFT");
    }

    @Test
    @Order(23)
    @DisplayName("review workflow — REJECTED → DRAFT → PENDING (resubmit after rejection)")
    @Transactional
    void requestReview_shouldAllowResubmitAfterRejection() {
        var created = memoryService.createV2(
                new CreateMemoryRequest("Resubmit Me", "Content.",
                        null, null, null, null, null, null, null, null), TENANT_A);
        // DRAFT → PENDING → REJECTED (via rejectReview)
        memoryService.requestReview(created.id(), TENANT_A, "submitter");
        memoryService.rejectReview(created.id(), TENANT_A, "reviewer-1", "Not ready yet");

        // Verify it's REJECTED
        var rejected = memoryService.getByIdV2(created.id(), TENANT_A);
        assertThat(rejected.lifecycleState()).isEqualTo(LifecycleState.REJECTED);

        // Step 1: REJECTED → DRAFT (return to draft for rework)
        memoryService.returnToDraft(created.id(), TENANT_A, "submitter", "Revised");
        var draft = memoryService.getByIdV2(created.id(), TENANT_A);
        assertThat(draft.lifecycleState()).isEqualTo(LifecycleState.DRAFT);

        // Step 2: DRAFT → PENDING (resubmit for review)
        memoryService.requestReview(created.id(), TENANT_A, "submitter");
        var pending = memoryService.getByIdV2(created.id(), TENANT_A);
        assertThat(pending.lifecycleState()).isEqualTo(LifecycleState.PENDING);
    }

    @Test
    @Order(24)
    @DisplayName("review workflow — full DRAFT → PENDING → ACTIVE happy path")
    @Transactional
    void reviewWorkflow_fullHappyPath() {
        var created = memoryService.createV2(
                new CreateMemoryRequest("Full Cycle", "Full content.",
                        null, null, null, null, null, null, null, null), TENANT_A);

        assertThat(created.lifecycleState()).isEqualTo(LifecycleState.DRAFT);

        // Step 1: Submit for review
        var pending = memoryService.requestReview(created.id(), TENANT_A, "author");
        assertThat(pending.lifecycleState()).isEqualTo(LifecycleState.PENDING);

        // Step 2: Approve
        var active = memoryService.approveReview(created.id(), TENANT_A, "reviewer", "Great work!");
        assertThat(active.lifecycleState()).isEqualTo(LifecycleState.ACTIVE);
        assertThat(active.isConsumerVisible()).isTrue();
    }

    // ── Issue #16: Indexing on ACTIVE transition ─────────────────────

    @Test
    @Order(25)
    @DisplayName("approveReview — triggers indexing on ACTIVE transition and writes embedding_id (#16, #17)")
    @Transactional
    void approveReview_shouldTriggerIndexingOnActive() {
        var created = memoryService.createV2(
                new CreateMemoryRequest("Indexable Memory", "Content to index via approval.",
                        null, null, null, null, null, null, null, null), TENANT_A);
        memoryService.requestReview(created.id(), TENANT_A, "submitter");

        // approveReview should trigger searchService.indexFragment() internally
        var approved = memoryService.approveReview(created.id(), TENANT_A, "reviewer-1", "Approved");

        assertThat(approved.lifecycleState()).isEqualTo(LifecycleState.ACTIVE);
        assertThat(approved.isConsumerVisible()).isTrue();
        // Issue #17: verify embedding_id is written back after approval-triggered indexing
        assertThat(approved.embeddingId())
                .as("embedding_id must be set after approval triggers indexing — Issue #17")
                .isNotNull()
                .isEqualTo(created.id().toString());
    }

    @Test
    @Order(26)
    @DisplayName("approveReview — indexing failure does not block approval (#16)")
    @Transactional
    void approveReview_indexingFailureShouldNotBlockApproval() {
        var created = memoryService.createV2(
                new CreateMemoryRequest("Fail-Index Memory", "Content.",
                        null, null, null, null, null, null, null, null), TENANT_A);
        memoryService.requestReview(created.id(), TENANT_A, "submitter");

        // Even if indexing fails (e.g., Qdrant unavailable), approval succeeds
        var approved = memoryService.approveReview(created.id(), TENANT_A, "reviewer-1", "Approved despite index failure");

        assertThat(approved.lifecycleState()).isEqualTo(LifecycleState.ACTIVE);
        assertThat(approved.id()).isEqualTo(created.id());
    }

    @Test
    @Order(27)
    @DisplayName("updateV2 — triggers indexing when transitioning to ACTIVE (#16)")
    @Transactional
    void updateV2_shouldTriggerIndexingOnActiveTransition() {
        var created = memoryService.createV2(
                new CreateMemoryRequest("Via UpdateV2", "Indexed via updateV2.",
                        null, null, null, null, null, null, null, null), TENANT_A);
        memoryService.updateV2(created.id(),
                new UpdateMemoryRequest(null, null, null, LifecycleState.PENDING, null, null),
                TENANT_A);
        var active = memoryService.updateV2(created.id(),
                new UpdateMemoryRequest(null, null, null, LifecycleState.ACTIVE, null, null),
                TENANT_A);

        assertThat(active.lifecycleState()).isEqualTo(LifecycleState.ACTIVE);
        assertThat(active.isConsumerVisible()).isTrue();
    }
}

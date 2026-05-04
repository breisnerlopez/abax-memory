package com.abax.memory.infrastructure.service;

import com.abax.memory.api.dto.v2.CreateMemoryRequest;
import com.abax.memory.api.dto.v2.MemoryResponse;
import com.abax.memory.api.dto.v2.UpdateMemoryRequest;
import com.abax.memory.domain.enums.LifecycleState;
import com.abax.memory.domain.enums.MemoryKind;
import com.abax.memory.domain.enums.SensitivityLevel;
import com.abax.memory.domain.service.AuditService;
import com.abax.memory.domain.service.MemoryService;
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

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Audit trail tests for EP-006: Governance and Traceability.
 *
 * <p>Verifies that:
 * <ul>
 *   <li>Creating, updating, and deleting a memory generates audit records.</li>
 *   <li>getAuditTrail returns the complete history ordered by created_at DESC.</li>
 *   <li>Audit records are immutable (append-only).</li>
 * </ul>
 *
 * <p>References: HU-006.1, HU-006.2, FT-006.01</p>
 */
@QuarkusTest
@TestProfile(H2TestProfile.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("EP-006: Audit Trail Tests")
class AuditTrailTest {

    private static final String TENANT = "audit-test-tenant";

    @Inject
    MemoryService memoryService;

    @Inject
    AuditService auditService;

    // ── D2.1: Create generates audit record ─────────────────────────

    @Test
    @Order(1)
    @DisplayName("D2.1 — Creating a memory generates an audit record")
    @Transactional
    void create_shouldGenerateAuditRecord() {
        var request = new CreateMemoryRequest(
                "Audit Create Test", "Content for audit verification.",
                MemoryKind.DECISION, "scope-audit", SensitivityLevel.INTERNAL,
                null, null, 0.8, null);
        MemoryResponse created = memoryService.createV2(request, TENANT);

        // Verify audit trail
        var trail = ((AuditServiceImpl) auditService).getAuditTrail(created.id(), TENANT);

        assertThat(trail).isNotEmpty();
        assertThat(trail.get(0).getAction()).isEqualTo("CREATE");
        assertThat(trail.get(0).getMemoryId()).isEqualTo(created.id());
        assertThat(trail.get(0).getTenantId()).isEqualTo(TENANT);
        assertThat(trail.get(0).getDiff()).isNotNull();
        assertThat(trail.get(0).getDiff()).containsKey("after");
        assertThat(trail.get(0).getCreatedAt()).isNotNull();
    }

    // ── D2.2: Update generates audit record ─────────────────────────

    @Test
    @Order(2)
    @DisplayName("D2.2 — Updating a memory generates an audit record")
    @Transactional
    void update_shouldGenerateAuditRecord() {
        var request = new CreateMemoryRequest(
                "Audit Update Original", "Original content.",
                MemoryKind.KNOWLEDGE, null, null, null, null, null, null);
        MemoryResponse created = memoryService.createV2(request, TENANT);

        // Update the memory
        var update = new UpdateMemoryRequest(
                "Audit Update Modified", "Modified content.",
                "New summary", null, SensitivityLevel.CONFIDENTIAL, 0.95);
        memoryService.updateV2(created.id(), update, TENANT);

        // Verify audit trail
        var trail = ((AuditServiceImpl) auditService).getAuditTrail(created.id(), TENANT);

        assertThat(trail).hasSize(2);
        // Most recent first
        assertThat(trail.get(0).getAction()).isEqualTo("UPDATE");
        assertThat(trail.get(0).getDiff()).containsKeys("before", "after");

        // Oldest: CREATE
        assertThat(trail.get(1).getAction()).isEqualTo("CREATE");
    }

    // ── D2.3: Soft-delete generates audit record ────────────────────

    @Test
    @Order(3)
    @DisplayName("D2.3 — Soft-deleting a memory generates an audit record")
    @Transactional
    void softDelete_shouldGenerateAuditRecord() {
        var request = new CreateMemoryRequest(
                "Audit Delete Test", "Will be deleted.",
                MemoryKind.DOCUMENT, null, null, null, null, null, null);
        MemoryResponse created = memoryService.createV2(request, TENANT);

        memoryService.softDeleteV2(created.id(), TENANT);

        // Verify audit trail
        var trail = ((AuditServiceImpl) auditService).getAuditTrail(created.id(), TENANT);

        assertThat(trail).hasSize(2);
        assertThat(trail.get(0).getAction()).isEqualTo("SOFT_DELETE");
        assertThat(trail.get(0).getDiff()).containsKey("previousState");
        assertThat(trail.get(1).getAction()).isEqualTo("CREATE");
    }

    // ── D2.4: Review workflow generates audit records ───────────────

    @Test
    @Order(4)
    @DisplayName("D2.4 — Review workflow generates proper audit records")
    @Transactional
    void reviewWorkflow_shouldGenerateAuditRecords() {
        var request = new CreateMemoryRequest(
                "Audit Review Test", "Needs review workflow.",
                MemoryKind.DECISION, null, SensitivityLevel.INTERNAL,
                null, null, 0.9, null);
        MemoryResponse created = memoryService.createV2(request, TENANT);

        // Submit for review
        memoryService.requestReview(created.id(), TENANT, "reviewer-1");

        // Approve
        memoryService.approveReview(created.id(), TENANT, "reviewer-1", "Verified. Correct.");

        // Verify trail
        var trail = ((AuditServiceImpl) auditService).getAuditTrail(created.id(), TENANT);

        assertThat(trail).hasSize(3);
        // Most recent: REVIEWED (approve)
        assertThat(trail.get(0).getAction()).isEqualTo("REVIEWED");
        assertThat(trail.get(0).getUserId()).isEqualTo("reviewer-1");
        @SuppressWarnings("unchecked")
        var approveDiff = (Map<String, Object>) trail.get(0).getDiff();
        assertThat(approveDiff).containsEntry("newState", "APPROVED");
        assertThat(approveDiff).containsEntry("comment", "Verified. Correct.");

        // Middle: REVIEW_REQUESTED
        assertThat(trail.get(1).getAction()).isEqualTo("REVIEW_REQUESTED");

        // Oldest: CREATE
        assertThat(trail.get(2).getAction()).isEqualTo("CREATE");
    }

    // ── D2.5: Audit records are immutable ───────────────────────────

    @Test
    @Order(5)
    @DisplayName("D2.5 — Audit records cannot be modified after creation")
    @Transactional
    void auditRecords_areImmutable() {
        var request = new CreateMemoryRequest(
                "Audit Immutable Test", "Immutable content.",
                MemoryKind.KNOWLEDGE, null, null, null, null, null, null);
        MemoryResponse created = memoryService.createV2(request, TENANT);

        // Get the audit trail
        var trailBefore = ((AuditServiceImpl) auditService).getAuditTrail(created.id(), TENANT);
        assertThat(trailBefore).hasSize(1);

        var originalTimestamp = trailBefore.get(0).getCreatedAt();
        var originalAction = trailBefore.get(0).getAction();

        // Verify the audit service has no update/delete methods
        // (The AuditService interface only exposes record, findByMemoryId, findByTenant)
        // Try to update the memory again and verify a NEW record is created (not modified)
        var update = new UpdateMemoryRequest("Changed Title", null, null, null, null, null);
        memoryService.updateV2(created.id(), update, TENANT);

        var trailAfter = ((AuditServiceImpl) auditService).getAuditTrail(created.id(), TENANT);
        assertThat(trailAfter).hasSize(2); // NEW record added

        // The original record must still exist unchanged
        var originalRecord = trailAfter.get(1);
        assertThat(originalRecord.getCreatedAt()).isEqualTo(originalTimestamp);
        assertThat(originalRecord.getAction()).isEqualTo(originalAction);
    }

    // ── D2.6: Audit trail is ordered by created_at descending ───────

    @Test
    @Order(6)
    @DisplayName("D2.6 — Audit trail is ordered by created_at DESC")
    @Transactional
    void auditTrail_isOrderedByCreatedAtDesc() {
        var request = new CreateMemoryRequest(
                "Ordered Audit Test", "Order check.",
                MemoryKind.DOCUMENT, null, null, null, null, null, null);
        MemoryResponse created = memoryService.createV2(request, TENANT);

        // Do two updates to create more audit records
        memoryService.updateV2(created.id(),
                new UpdateMemoryRequest("Update 1", null, null, null, null, null), TENANT);
        memoryService.updateV2(created.id(),
                new UpdateMemoryRequest("Update 2", null, null, null, null, null), TENANT);

        var trail = ((AuditServiceImpl) auditService).getAuditTrail(created.id(), TENANT);
        assertThat(trail).hasSize(3);

        // Verify descending order: most recent first
        for (int i = 1; i < trail.size(); i++) {
            assertThat(trail.get(i - 1).getCreatedAt())
                    .isAfterOrEqualTo(trail.get(i).getCreatedAt());
        }
    }
}

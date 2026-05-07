package com.abax.memory.infrastructure.service;

import com.abax.memory.api.dto.v2.CreateMemoryRequest;
import com.abax.memory.api.dto.v2.MemoryResponse;
import com.abax.memory.api.dto.v2.PatchRelationRequest;
import com.abax.memory.api.dto.v2.UpdateRelationRequest;
import com.abax.memory.domain.enums.RelationType;
import com.abax.memory.domain.model.Relation;
import com.abax.memory.domain.service.MemoryService;
import com.abax.memory.domain.service.RelationService;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Service-layer tests for {@link RelationServiceImpl} — EP-005.
 *
 * <p>Tests create, delete, listing by direction, self-relation
 * rejection, cross-tenant prevention, and duplicate detection.</p>
 *
 * <p>References: HU-001.8.1, HU-001.8.2</p>
 */
@QuarkusTest
@TestProfile(H2TestProfile.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("RelationServiceImpl — relation business logic tests")
class RelationServiceImplTest {

    private static final String TENANT_A = "tenant-a-relations";
    private static final String TENANT_B = "tenant-b-relations";

    @Inject
    RelationService relationService;

    @Inject
    MemoryService memoryService;

    // ── Create Relation Tests ────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("createRelation — creates a valid directed relationship")
    @Transactional
    void createRelation_shouldCreateDirectedRelationship() {
        var source = createMemory(TENANT_A, "Source Fragment", "Source content.");
        var target = createMemory(TENANT_A, "Target Fragment", "Target content.");

        Relation relation = relationService.createRelation(
                source.id(), target.id(), RelationType.DEPENDS_ON, TENANT_A);

        assertThat(relation.getId()).isNotNull();
        assertThat(relation.getSourceId()).isEqualTo(source.id());
        assertThat(relation.getTargetId()).isEqualTo(target.id());
        assertThat(relation.getType()).isEqualTo(RelationType.DEPENDS_ON);
        assertThat(relation.getTenantId()).isEqualTo(TENANT_A);
        assertThat(relation.getCreatedAt()).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("createRelation — rejects self-relations")
    @Transactional
    void createRelation_shouldRejectSelfRelation() {
        var fragment = createMemory(TENANT_A, "Self Fragment", "Cannot relate to self.");

        assertThatThrownBy(() -> relationService.createRelation(
                fragment.id(), fragment.id(), RelationType.RELATED_TO, TENANT_A))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("self-relation");
    }

    @Test
    @Order(3)
    @DisplayName("createRelation — rejects non-existent target")
    @Transactional
    void createRelation_shouldRejectNonExistentTarget() {
        var source = createMemory(TENANT_A, "Valid Source", "Valid source with missing target.");

        assertThatThrownBy(() -> relationService.createRelation(
                source.id(), UUID.randomUUID(), RelationType.MENTIONS, TENANT_A))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @Order(4)
    @DisplayName("createRelation — rejects non-existent source")
    @Transactional
    void createRelation_shouldRejectNonExistentSource() {
        var target = createMemory(TENANT_A, "Valid Target", "Valid target with missing source.");

        assertThatThrownBy(() -> relationService.createRelation(
                UUID.randomUUID(), target.id(), RelationType.MENTIONS, TENANT_A))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @Order(5)
    @DisplayName("createRelation — rejects cross-tenant relation")
    @Transactional
    void createRelation_shouldRejectCrossTenantRelation() {
        var source = createMemory(TENANT_A, "Tenant A Source", "Belongs to tenant A.");
        var target = createMemory(TENANT_B, "Tenant B Target", "Belongs to tenant B.");

        // Creating from tenant A's perspective but target is tenant B → should fail
        assertThatThrownBy(() -> relationService.createRelation(
                source.id(), target.id(), RelationType.RELATED_TO, TENANT_A))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @Order(6)
    @DisplayName("createRelation — rejects duplicate relation")
    @Transactional
    void createRelation_shouldRejectDuplicate() {
        var source = createMemory(TENANT_A, "Dup Source", "Source for duplicate test.");
        var target = createMemory(TENANT_A, "Dup Target", "Target for duplicate test.");

        relationService.createRelation(source.id(), target.id(), RelationType.SUPPORTS, TENANT_A);

        assertThatThrownBy(() -> relationService.createRelation(
                source.id(), target.id(), RelationType.SUPPORTS, TENANT_A))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    // ── Delete Relation Tests ────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("deleteRelation — deletes an existing relation")
    @Transactional
    void deleteRelation_shouldDeleteExistingRelation() {
        var source = createMemory(TENANT_A, "Delete Source", "Will be deleted.");
        var target = createMemory(TENANT_A, "Delete Target", "Target for delete.");

        Relation rel = relationService.createRelation(
                source.id(), target.id(), RelationType.MENTIONS, TENANT_A);

        relationService.deleteRelation(rel.getId(), TENANT_A);

        // Verify deletion — getRelations should return nothing
        List<Relation> remaining = relationService.getRelations(source.id(), "both", TENANT_A);
        assertThat(remaining).noneMatch(r -> r.getId().equals(rel.getId()));
    }

    @Test
    @Order(8)
    @DisplayName("deleteRelation — throws 404 for non-existent relation")
    @Transactional
    void deleteRelation_shouldThrow404ForNonExistent() {
        assertThatThrownBy(() -> relationService.deleteRelation(UUID.randomUUID(), TENANT_A))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @Order(9)
    @DisplayName("deleteRelation — throws 404 for cross-tenant delete")
    @Transactional
    void deleteRelation_shouldThrow404ForCrossTenant() {
        var source = createMemory(TENANT_A, "Cross Delete Source", "Source.");
        var target = createMemory(TENANT_A, "Cross Delete Target", "Target.");

        Relation rel = relationService.createRelation(
                source.id(), target.id(), RelationType.MENTIONS, TENANT_A);

        // Tenant B trying to delete tenant A's relation
        assertThatThrownBy(() -> relationService.deleteRelation(rel.getId(), TENANT_B))
                .isInstanceOf(NotFoundException.class);
    }

    // ── Get Relations Tests ──────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("getRelations — returns outgoing relations correctly")
    @Transactional
    void getRelations_outgoing_shouldReturnOnlyOutgoing() {
        var source = createMemory(TENANT_A, "Outgoing Source", "Outgoing test.");
        var target = createMemory(TENANT_A, "Outgoing Target", "Target.");

        relationService.createRelation(source.id(), target.id(), RelationType.DEPENDS_ON, TENANT_A);

        List<Relation> relations = relationService.getRelations(source.id(), "outgoing", TENANT_A);

        assertThat(relations).hasSize(1);
        assertThat(relations.get(0).getSourceId()).isEqualTo(source.id());
        assertThat(relations.get(0).getTargetId()).isEqualTo(target.id());
    }

    @Test
    @Order(11)
    @DisplayName("getRelations — returns incoming relations correctly")
    @Transactional
    void getRelations_incoming_shouldReturnOnlyIncoming() {
        var source = createMemory(TENANT_A, "Incoming Source", "Source.");
        var target = createMemory(TENANT_A, "Incoming Target", "Target.");

        relationService.createRelation(source.id(), target.id(), RelationType.DEPENDS_ON, TENANT_A);

        List<Relation> relations = relationService.getRelations(target.id(), "incoming", TENANT_A);

        assertThat(relations).hasSize(1);
        assertThat(relations.get(0).getSourceId()).isEqualTo(source.id());
    }

    @Test
    @Order(12)
    @DisplayName("getRelations — returns both directions correctly")
    @Transactional
    void getRelations_both_shouldReturnAllDirections() {
        var center = createMemory(TENANT_A, "Center Node", "Center.");
        var child1 = createMemory(TENANT_A, "Child 1", "First child.");
        var parent = createMemory(TENANT_A, "Parent Node", "Parent.");

        relationService.createRelation(center.id(), child1.id(), RelationType.SUPPORTS, TENANT_A);
        relationService.createRelation(parent.id(), center.id(), RelationType.DEPENDS_ON, TENANT_A);

        List<Relation> relations = relationService.getRelations(center.id(), "both", TENANT_A);

        assertThat(relations).hasSize(2);
    }

    @Test
    @Order(13)
    @DisplayName("getRelations — returns empty for fragment with no relations")
    @Transactional
    void getRelations_shouldReturnEmptyForNoRelations() {
        var fragment = createMemory(TENANT_A, "Lonely Fragment", "No friends.");

        List<Relation> relations = relationService.getRelations(fragment.id(), "both", TENANT_A);

        assertThat(relations).isEmpty();
    }

    // ── Domain-model method tests ────────────────────────────────────

    @Test
    @Order(14)
    @DisplayName("findBySource — returns relations where fragment is source")
    @Transactional
    void findBySource_shouldReturnSourceRelations() {
        var source = createMemory(TENANT_A, "FindBySource Test", "Source.");
        var target = createMemory(TENANT_A, "FindBySource Target", "Target.");

        relationService.createRelation(source.id(), target.id(), RelationType.MENTIONS, TENANT_A);

        List<Relation> relations = relationService.findBySource(source.id());

        assertThat(relations).hasSize(1);
        assertThat(relations.get(0).getSourceId()).isEqualTo(source.id());
    }

    @Test
    @Order(15)
    @DisplayName("findByTarget — returns relations where fragment is target")
    @Transactional
    void findByTarget_shouldReturnTargetRelations() {
        var source = createMemory(TENANT_A, "FindByTarget Source", "Source.");
        var target = createMemory(TENANT_A, "FindByTarget Target", "Target.");

        relationService.createRelation(source.id(), target.id(), RelationType.SUPPORTS, TENANT_A);

        List<Relation> relations = relationService.findByTarget(target.id());

        assertThat(relations).hasSize(1);
        assertThat(relations.get(0).getTargetId()).isEqualTo(target.id());
    }

    // ── v2.1.0: Update and Patch Tests (CP-V21-024) ──────────────────

    @Test
    @Order(16)
    @DisplayName("updateRelation — fully replaces type, weight, and metadata")
    @Transactional
    void updateRelation_shouldFullyReplaceFields() {
        var source = createMemory(TENANT_A, "Update Source", "Source for update.");
        var target = createMemory(TENANT_A, "Update Target", "Target for update.");

        Relation rel = relationService.createRelation(
                source.id(), target.id(), RelationType.MENTIONS, TENANT_A);
        // Weight defaults to 1.0 from the entity column default
        assertThat(rel.getWeight()).isEqualTo(1.0);

        var metadata = Map.<String, Object>of("reason", "updated", "priority", 1);
        var request = new UpdateRelationRequest(
                RelationType.SUPPORTS, 0.85, metadata);
        Relation updated = relationService.updateRelation(rel.getId(), request, TENANT_A, "test-actor");

        assertThat(updated.getType()).isEqualTo(RelationType.SUPPORTS);
        assertThat(updated.getWeight()).isEqualTo(0.85);
        assertThat(updated.getMetadata()).containsEntry("reason", "updated");
        assertThat(updated.getMetadata()).containsEntry("priority", 1);
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    @Test
    @Order(17)
    @DisplayName("updateRelation — throws 404 for non-existent relation")
    @Transactional
    void updateRelation_shouldThrow404ForNonExistent() {
        var request = new UpdateRelationRequest(RelationType.RELATED_TO, 1.0, null);
        assertThatThrownBy(() -> relationService.updateRelation(
                UUID.randomUUID(), request, TENANT_A, "test-actor"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @Order(18)
    @DisplayName("updateRelation — throws 404 for cross-tenant update")
    @Transactional
    void updateRelation_shouldThrow404ForCrossTenant() {
        var source = createMemory(TENANT_A, "Cross Update Src", "Source.");
        var target = createMemory(TENANT_A, "Cross Update Tgt", "Target.");

        Relation rel = relationService.createRelation(
                source.id(), target.id(), RelationType.MENTIONS, TENANT_A);

        var request = new UpdateRelationRequest(RelationType.SUPPORTS, 1.0, null);
        assertThatThrownBy(() -> relationService.updateRelation(
                rel.getId(), request, TENANT_B, "test-actor"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @Order(19)
    @DisplayName("patchRelation — updates only provided fields")
    @Transactional
    void patchRelation_shouldUpdateOnlyProvidedFields() {
        var source = createMemory(TENANT_A, "Patch Source", "Source for patch.");
        var target = createMemory(TENANT_A, "Patch Target", "Target for patch.");

        Relation rel = relationService.createRelation(
                source.id(), target.id(), RelationType.MENTIONS, TENANT_A);

        // Patch only the type
        var patchRequest = new PatchRelationRequest(RelationType.DEPENDS_ON, null, null);
        Relation patched = relationService.patchRelation(rel.getId(), patchRequest, TENANT_A, "test-actor");

        assertThat(patched.getType()).isEqualTo(RelationType.DEPENDS_ON);
        // Source/target IDs unchanged
        assertThat(patched.getSourceId()).isEqualTo(source.id());
        assertThat(patched.getTargetId()).isEqualTo(target.id());
    }

    @Test
    @Order(20)
    @DisplayName("patchRelation — updates weight only")
    @Transactional
    void patchRelation_shouldUpdateWeightOnly() {
        var source = createMemory(TENANT_A, "Patch Weight Src", "Source.");
        var target = createMemory(TENANT_A, "Patch Weight Tgt", "Target.");

        Relation rel = relationService.createRelation(
                source.id(), target.id(), RelationType.SUPPORTS, TENANT_A);

        var patchRequest = new PatchRelationRequest(null, 0.5, null);
        Relation patched = relationService.patchRelation(rel.getId(), patchRequest, TENANT_A, "test-actor");

        assertThat(patched.getWeight()).isEqualTo(0.5);
        assertThat(patched.getType()).isEqualTo(RelationType.SUPPORTS); // unchanged
    }

    @Test
    @Order(21)
    @DisplayName("patchRelation — updates metadata only")
    @Transactional
    void patchRelation_shouldUpdateMetadataOnly() {
        var source = createMemory(TENANT_A, "Patch Meta Src", "Source.");
        var target = createMemory(TENANT_A, "Patch Meta Tgt", "Target.");

        Relation rel = relationService.createRelation(
                source.id(), target.id(), RelationType.BELONGS_TO, TENANT_A);

        var metadata = Map.<String, Object>of("confidence", 0.9, "source", "auto");
        var patchRequest = new PatchRelationRequest(null, null, metadata);
        Relation patched = relationService.patchRelation(rel.getId(), patchRequest, TENANT_A, "test-actor");

        assertThat(patched.getMetadata()).containsEntry("confidence", 0.9);
        assertThat(patched.getMetadata()).containsEntry("source", "auto");
        assertThat(patched.getType()).isEqualTo(RelationType.BELONGS_TO); // unchanged
    }

    @Test
    @Order(22)
    @DisplayName("patchRelation — throws error when no fields provided")
    @Transactional
    void patchRelation_shouldThrowWhenNoFieldsProvided() {
        var source = createMemory(TENANT_A, "Patch Empty Src", "Source.");
        var target = createMemory(TENANT_A, "Patch Empty Tgt", "Target.");

        Relation rel = relationService.createRelation(
                source.id(), target.id(), RelationType.MENTIONS, TENANT_A);

        var patchRequest = new PatchRelationRequest(null, null, null);
        assertThatThrownBy(() -> relationService.patchRelation(
                rel.getId(), patchRequest, TENANT_A, "test-actor"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one field");
    }

    @Test
    @Order(23)
    @DisplayName("patchRelation — throws 404 for non-existent relation")
    @Transactional
    void patchRelation_shouldThrow404ForNonExistent() {
        var patchRequest = new PatchRelationRequest(RelationType.RELATED_TO, null, null);
        assertThatThrownBy(() -> relationService.patchRelation(
                UUID.randomUUID(), patchRequest, TENANT_A, "test-actor"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @Order(24)
    @DisplayName("updateRelation — audit trail is recorded on update")
    @Transactional
    void updateRelation_shouldRecordAuditTrail() {
        var source = createMemory(TENANT_A, "Audit Update Src", "Source.");
        var target = createMemory(TENANT_A, "Audit Update Tgt", "Target.");

        Relation rel = relationService.createRelation(
                source.id(), target.id(), RelationType.MENTIONS, TENANT_A);

        var request = new UpdateRelationRequest(
                RelationType.CAUSED_BY, 0.75, Map.<String, Object>of("audit_test", true));
        Relation updated = relationService.updateRelation(rel.getId(), request, TENANT_A, "audit-actor");

        assertThat(updated.getUpdatedAt()).isNotNull();
        assertThat(updated.getType()).isEqualTo(RelationType.CAUSED_BY);
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private MemoryResponse createMemory(String tenantId, String title, String content) {
        return memoryService.createV2(
                new CreateMemoryRequest(title, content, null, null, null, null, null, null, null, null),
                tenantId);
    }
}

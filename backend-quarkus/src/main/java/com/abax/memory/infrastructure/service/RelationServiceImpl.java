package com.abax.memory.infrastructure.service;

import com.abax.memory.api.dto.v2.PatchRelationRequest;
import com.abax.memory.api.dto.v2.UpdateRelationRequest;
import com.abax.memory.domain.enums.RelationType;
import com.abax.memory.domain.model.GraphMutatedEvent;
import com.abax.memory.domain.model.Relation;
import com.abax.memory.domain.service.AuditService;
import com.abax.memory.domain.service.RelationService;
import com.abax.memory.infrastructure.persistence.MemoryFragmentEntity;
import com.abax.memory.infrastructure.persistence.RelationEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of {@link RelationService} using Panache active-record
 * persistence with PostgreSQL.
 *
 * <p>Validates that source and target fragments exist and belong to
 * the same tenant. Prevents self-relations.</p>
 *
 * <p>References: EP-005, HU-001.8.1, HU-001.8.2, Flyway V3</p>
 */
@ApplicationScoped
public class RelationServiceImpl implements RelationService {

    private static final Logger LOG = Logger.getLogger(RelationServiceImpl.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    Event<GraphMutatedEvent> graphMutationEvent;

    @Inject
    AuditService auditService;

    // ── Domain-model methods ─────────────────────────────────────────

    @Override
    @Transactional
    public Relation create(Relation relation) {
        if (relation.getSourceId().equals(relation.getTargetId())) {
            throw new IllegalArgumentException("Cannot create self-relation: source and target must differ");
        }

        var entity = toEntity(relation);
        entity.persist();
        LOG.debugv("Relation created: id={0}, source={1}, target={2}, type={3}",
                entity.getId(), entity.getSourceId(), entity.getTargetId(), entity.getRelationType());
        return toDomain(entity);
    }

    @Override
    @Transactional
    public void delete(UUID relationId) {
        var entity = (RelationEntity) RelationEntity.findById(relationId);
        if (entity != null) {
            entity.delete();
            LOG.debugv("Relation deleted: id={0}", relationId);
        }
    }

    @Override
    public List<Relation> findBySource(UUID memoryId) {
        var entities = RelationEntity.find("sourceId", memoryId).list();
        return entities.stream()
                .map(e -> toDomain((RelationEntity) e))
                .toList();
    }

    @Override
    public List<Relation> findByTarget(UUID memoryId) {
        var entities = RelationEntity.find("targetId", memoryId).list();
        return entities.stream()
                .map(e -> toDomain((RelationEntity) e))
                .toList();
    }

    // ── EP-005: Tenant-aware REST methods ────────────────────────────

    @Override
    @Transactional
    public Relation createRelation(UUID sourceId, UUID targetId, RelationType type, String tenantId) {
        // Validate no self-relation
        if (sourceId.equals(targetId)) {
            throw new IllegalArgumentException("Cannot create self-relation: source and target must differ");
        }

        // Validate both fragments exist and belong to the same tenant
        MemoryFragmentEntity source = requireEntityForTenant(sourceId, tenantId);
        MemoryFragmentEntity target = requireEntityForTenant(targetId, tenantId);

        // Check for duplicate
        long existing = RelationEntity.count(
                "sourceId = :sourceId and targetId = :targetId and relationType = :type",
                Map.of("sourceId", sourceId, "targetId", targetId, "type", type));
        if (existing > 0) {
            throw new IllegalArgumentException(
                    "Relation already exists: " + sourceId + " → " + targetId + " (" + type + ")");
        }

        var entity = new RelationEntity();
        entity.setSourceId(sourceId);
        entity.setTargetId(targetId);
        entity.setRelationType(type);
        entity.setTenantId(tenantId);
        entity.persist();

        // FT-V21-002.1: Fire graph mutation event for cache invalidation
        graphMutationEvent.fireAsync(new GraphMutatedEvent(sourceId, targetId, "CREATE"));

        LOG.infov("Relation created via API v2: id={0}, source={1}, target={2}, type={3}, tenant={4}",
                entity.getId(), sourceId, targetId, type, tenantId);
        return toDomain(entity);
    }

    @Override
    @Transactional
    public void deleteRelation(UUID relationId, String tenantId) {
        var entity = (RelationEntity) RelationEntity.findById(relationId);
        if (entity == null) {
            throw new NotFoundException("Relation not found: " + relationId);
        }
        if (!tenantId.equals(entity.getTenantId())) {
            LOG.warnv("Cross-tenant relation delete attempt: id={0}, requesting_tenant={1}, resource_tenant={2}",
                    relationId, tenantId, entity.getTenantId());
            throw new NotFoundException("Relation not found: " + relationId);
        }

        UUID sourceId = entity.getSourceId();
        UUID targetId = entity.getTargetId();
        entity.delete();

        // FT-V21-002.1: Fire graph mutation event for cache invalidation
        graphMutationEvent.fireAsync(new GraphMutatedEvent(sourceId, targetId, "DELETE"));

        LOG.infov("Relation deleted via API v2: id={0}, tenant={1}", relationId, tenantId);
    }

    @Override
    public List<Relation> getRelations(UUID fragmentId, String direction, String tenantId) {
        List<Relation> relations = new ArrayList<>();

        boolean includeOutgoing = "outgoing".equalsIgnoreCase(direction) || "both".equalsIgnoreCase(direction);
        boolean includeIncoming = "incoming".equalsIgnoreCase(direction) || "both".equalsIgnoreCase(direction);

        if (includeOutgoing) {
            var outgoing = RelationEntity.find(
                    "sourceId = :fragmentId and tenantId = :tenantId",
                    Map.of("fragmentId", fragmentId, "tenantId", tenantId))
                    .list();
            relations.addAll(outgoing.stream()
                    .map(e -> toDomain((RelationEntity) e))
                    .toList());
        }

        if (includeIncoming) {
            var incoming = RelationEntity.find(
                    "targetId = :fragmentId and tenantId = :tenantId",
                    Map.of("fragmentId", fragmentId, "tenantId", tenantId))
                    .list();
            relations.addAll(incoming.stream()
                    .map(e -> toDomain((RelationEntity) e))
                    .toList());
        }

        return relations;
    }

    // ── v2.1.0: Update and patch methods (CP-V21-024) ────────────────

    @Override
    @Transactional
    public Relation updateRelation(UUID relationId, UpdateRelationRequest request, String tenantId, String actorId) {
        var entity = requireRelationForTenant(relationId, tenantId);

        // Capture before-state for audit
        Map<String, Object> before = relationToAuditSnapshot(entity);

        // Apply changes
        entity.setRelationType(request.relationType());
        entity.setWeight(request.weight());
        entity.setMetadata(serializeMetadata(request.metadata()));
        entity.persist();

        // Fire graph mutation event for cache invalidation
        graphMutationEvent.fireAsync(new GraphMutatedEvent(entity.getSourceId(), entity.getTargetId(), "UPDATE"));

        // Record audit
        Map<String, Object> after = relationToAuditSnapshot(entity);
        Map<String, Object> diff = buildDiff(before, after);
        ((AuditServiceImpl) auditService).recordAction(
                entity.getSourceId(), // primary fragment for audit
                "RELATION_UPDATE",
                actorId != null ? actorId : "system",
                tenantId,
                diff);

        LOG.infov("Relation updated via PUT v2: id={0}, type={1}, tenant={2}",
                relationId, entity.getRelationType(), tenantId);
        return toDomain(entity);
    }

    @Override
    @Transactional
    public Relation patchRelation(UUID relationId, PatchRelationRequest request, String tenantId, String actorId) {
        if (!request.hasAnyField()) {
            throw new IllegalArgumentException("At least one field must be provided for patch update");
        }

        var entity = requireRelationForTenant(relationId, tenantId);

        // Capture before-state for audit
        Map<String, Object> before = relationToAuditSnapshot(entity);

        // Apply only non-null fields
        if (request.relationType() != null) {
            entity.setRelationType(request.relationType());
        }
        if (request.weight() != null) {
            entity.setWeight(request.weight());
        }
        if (request.metadata() != null) {
            entity.setMetadata(serializeMetadata(request.metadata()));
        }
        entity.persist();

        // Fire graph mutation event for cache invalidation
        graphMutationEvent.fireAsync(new GraphMutatedEvent(entity.getSourceId(), entity.getTargetId(), "UPDATE"));

        // Record audit
        Map<String, Object> after = relationToAuditSnapshot(entity);
        Map<String, Object> diff = buildDiff(before, after);
        ((AuditServiceImpl) auditService).recordAction(
                entity.getSourceId(),
                "RELATION_UPDATE",
                actorId != null ? actorId : "system",
                tenantId,
                diff);

        LOG.infov("Relation updated via PATCH v2: id={0}, tenant={1}", relationId, tenantId);
        return toDomain(entity);
    }

    // ── Private Helpers ──────────────────────────────────────────────

    /**
     * Retrieves a relation entity scoped to the given tenant.
     *
     * @throws NotFoundException if the relation does not exist or belongs to another tenant
     */
    private RelationEntity requireRelationForTenant(UUID relationId, String tenantId) {
        var entity = (RelationEntity) RelationEntity.findById(relationId);
        if (entity == null) {
            throw new NotFoundException("Relation not found: " + relationId);
        }
        if (!tenantId.equals(entity.getTenantId())) {
            LOG.warnv("Cross-tenant relation access: id={0}, requesting_tenant={1}, resource_tenant={2}",
                    relationId, tenantId, entity.getTenantId());
            throw new NotFoundException("Relation not found: " + relationId);
        }
        return entity;
    }

    /**
     * Builds a snapshot of the relation for audit-comparison purposes.
     */
    private Map<String, Object> relationToAuditSnapshot(RelationEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("relationType", entity.getRelationType() != null ? entity.getRelationType().name() : null);
        snapshot.put("weight", entity.getWeight());
        snapshot.put("metadata", entity.getMetadata());
        return snapshot;
    }

    /**
     * Builds a diff map containing only the fields that changed.
     */
    private Map<String, Object> buildDiff(Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> diff = new HashMap<>();
        diff.put("before", before);
        diff.put("after", after);
        // Compute changed fields
        List<String> changed = new ArrayList<>();
        for (String key : before.keySet()) {
            Object beforeVal = before.get(key);
            Object afterVal = after.get(key);
            if (!java.util.Objects.equals(beforeVal, afterVal)) {
                changed.add(key);
            }
        }
        diff.put("changedFields", changed);
        return diff;
    }

    /**
     * Serializes a metadata map to a JSON string for JSONB storage.
     */
    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        try {
            return MAPPER.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            LOG.errorv("Failed to serialize relation metadata: {0}", e.getMessage());
            return "{}";
        }
    }

    private MemoryFragmentEntity requireEntityForTenant(UUID id, String tenantId) {
        var entity = (MemoryFragmentEntity) MemoryFragmentEntity.findById(id);
        if (entity == null || entity.isDeleted()) {
            throw new NotFoundException("Memory fragment not found: " + id);
        }
        if (!tenantId.equals(entity.getTenantId())) {
            LOG.warnv("Cross-tenant fragment access in relation: id={0}, requesting_tenant={1}, resource_tenant={2}",
                    id, tenantId, entity.getTenantId());
            throw new NotFoundException("Memory fragment not found: " + id);
        }
        return entity;
    }

    // ── Entity ↔ Domain mapping ─────────────────────────────────────

    private RelationEntity toEntity(Relation domain) {
        var entity = new RelationEntity();
        entity.setId(domain.getId());
        entity.setSourceId(domain.getSourceId());
        entity.setTargetId(domain.getTargetId());
        entity.setRelationType(domain.getType());
        entity.setWeight(domain.getWeight());
        entity.setMetadata(serializeMetadata(domain.getMetadata()));
        entity.setTenantId(domain.getTenantId());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    private Relation toDomain(RelationEntity entity) {
        var domain = new Relation();
        domain.setId(entity.getId());
        domain.setSourceId(entity.getSourceId());
        domain.setTargetId(entity.getTargetId());
        domain.setType(entity.getRelationType());
        domain.setWeight(entity.getWeight());
        domain.setMetadata(deserializeMetadata(entity.getMetadata()));
        domain.setTenantId(entity.getTenantId());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        return domain;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deserializeMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank() || "{}".equals(metadataJson)) {
            return Map.of();
        }
        try {
            return MAPPER.readValue(metadataJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            LOG.errorv("Failed to deserialize relation metadata: {0}", e.getMessage());
            return Map.of();
        }
    }
}

package com.abax.memory.infrastructure.service;

import com.abax.memory.domain.enums.RelationType;
import com.abax.memory.domain.model.Relation;
import com.abax.memory.domain.service.RelationService;
import com.abax.memory.infrastructure.persistence.MemoryFragmentEntity;
import com.abax.memory.infrastructure.persistence.RelationEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.util.ArrayList;
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

        entity.delete();
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

    // ── Private Helpers ──────────────────────────────────────────────

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
        entity.setTenantId(domain.getTenantId());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    private Relation toDomain(RelationEntity entity) {
        var domain = new Relation();
        domain.setId(entity.getId());
        domain.setSourceId(entity.getSourceId());
        domain.setTargetId(entity.getTargetId());
        domain.setType(entity.getRelationType());
        domain.setTenantId(entity.getTenantId());
        domain.setCreatedAt(entity.getCreatedAt());
        return domain;
    }
}

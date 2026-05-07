package com.abax.memory.infrastructure.service;

import com.abax.memory.domain.model.DeleteNamespaceResult;
import com.abax.memory.domain.service.NamespaceService;
import com.abax.memory.infrastructure.persistence.MemoryFragmentEntity;
import com.abax.memory.infrastructure.persistence.RelationEntity;
import com.abax.memory.infrastructure.qdrant.QdrantClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of {@link NamespaceService} — v2.1.0.
 *
 * <p>Deletes a namespace atomically:
 * <ol>
 *   <li>Count and delete memories in the namespace (PostgreSQL, transactional)</li>
 *   <li>Count and delete relations referencing deleted memories</li>
 *   <li>Delete Qdrant points by tenant_id + namespace filter</li>
 *   <li>If Qdrant delete fails, log ERROR critical (PostgreSQL already committed)</li>
 * </ol>
 * </p>
 *
 * <p>References: ADR-013, FT-V21-004.3</p>
 */
@ApplicationScoped
public class NamespaceServiceImpl implements NamespaceService {

    private static final Logger LOG = Logger.getLogger(NamespaceServiceImpl.class);

    @Inject
    QdrantClient qdrantClient;

    @Inject
    AuditServiceImpl auditService;

    @ConfigProperty(name = "abax.v2.qdrant.collection", defaultValue = "abax-memories")
    String qdrantCollection;

    @Override
    @Transactional
    public DeleteNamespaceResult deleteNamespace(String namespace, String tenantId) {
        LOG.infov("Namespace deletion started: namespace={0}, tenant={1}", namespace, tenantId);

        // 1. Count memories in namespace
        long memoryCount = MemoryFragmentEntity.count(
                "tenantId = :tenantId AND namespace = :namespace AND deletedAt IS NULL",
                Map.of("tenantId", tenantId, "namespace", namespace));

        if (memoryCount == 0) {
            // Check if namespace ever existed (including soft-deleted)
            long everExisted = MemoryFragmentEntity.count(
                    "tenantId = :tenantId AND namespace = :namespace",
                    Map.of("tenantId", tenantId, "namespace", namespace));
            if (everExisted == 0) {
                throw new NotFoundException(
                        "Namespace '" + namespace + "' not found for tenant '" + tenantId + "'");
            }
            // All memories were already soft-deleted; return empty result
            LOG.infov("Namespace {0} already empty (all memories soft-deleted)", namespace);
            return new DeleteNamespaceResult(namespace, tenantId, 0, 0, 0);
        }

        // 2. Collect memory IDs to delete (for relation cleanup and Qdrant cleanup)
        List<MemoryFragmentEntity> toDelete = MemoryFragmentEntity.find(
                "tenantId = :tenantId AND namespace = :namespace AND deletedAt IS NULL",
                Map.of("tenantId", tenantId, "namespace", namespace))
                .list();

        List<UUID> memoryIds = toDelete.stream()
                .map(e -> ((MemoryFragmentEntity) e).getId())
                .toList();

        LOG.debugv("Namespace {0}: {1} active memories to delete", namespace, memoryIds.size());

        // 3. Count and delete relations connected to these memories
        long deletedRelations = 0;
        if (!memoryIds.isEmpty()) {
            deletedRelations = RelationEntity.delete(
                    "(sourceId IN ?1 OR targetId IN ?1) AND tenantId = ?2",
                    memoryIds, tenantId);
            LOG.debugv("Namespace {0}: {1} relations deleted", namespace, deletedRelations);
        }

        // 4. Soft-delete all memories (bulk update for atomicity)
        for (MemoryFragmentEntity entity : toDelete) {
            MemoryFragmentEntity e = (MemoryFragmentEntity) entity;
            e.softDelete();
            e.persist();
        }
        LOG.infov("Namespace {0}: {1} memories soft-deleted", namespace, memoryCount);

        // 5. Delete Qdrant points (after PG commit — best-effort)
        long deletedQdrantPoints = 0;
        try {
            Map<String, Object> qdrantFilters = Map.of(
                    "tenant_id", tenantId,
                    "namespace", namespace
            );
            deletedQdrantPoints = qdrantClient.deleteByFilter(qdrantCollection, qdrantFilters);
            LOG.infov("Namespace {0}: {1} Qdrant points deleted", namespace, deletedQdrantPoints);
        } catch (Exception e) {
            LOG.errorv(e, "CRITICAL: Failed to delete Qdrant points for namespace {0}. "
                    + "PostgreSQL records already deleted. Manual cleanup required.", namespace);
        }

        // 6. Audit the deletion
        auditService.recordAction(
                UUID.nameUUIDFromBytes(namespace.getBytes()),
                "NAMESPACE_DELETE", "admin", tenantId,
                Map.of("namespace", namespace,
                        "deletedMemories", memoryCount,
                        "deletedRelations", deletedRelations,
                        "deletedQdrantPoints", deletedQdrantPoints));

        LOG.infov("Namespace deletion complete: namespace={0}, tenant={1}, "
                + "memories={2}, relations={3}, qdrant={4}",
                namespace, tenantId, memoryCount, deletedRelations, deletedQdrantPoints);

        return new DeleteNamespaceResult(namespace, tenantId,
                memoryCount, deletedRelations, deletedQdrantPoints);
    }
}

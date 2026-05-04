package com.abax.memory.infrastructure.service;

import com.abax.memory.domain.model.AuditRecord;
import com.abax.memory.domain.service.AuditService;
import com.abax.memory.infrastructure.persistence.AuditRecordEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of {@link AuditService} — append-only audit trail
 * backed by the {@code audit_records} PostgreSQL table.
 *
 * <h3>Immutability</h3>
 * Audit records are append-only. No update or delete operations
 * are exposed or permitted.
 *
 * <p>References: EP-006, Architecture document §7.1, Flyway V4</p>
 */
@ApplicationScoped
public class AuditServiceImpl implements AuditService {

    private static final Logger LOG = Logger.getLogger(AuditServiceImpl.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── AuditService contract ────────────────────────────────────────

    @Override
    @Transactional
    public AuditRecord record(AuditRecord event) {
        var entity = toEntity(event);
        entity.persist();
        LOG.infov("Audit record created: id={0}, memoryId={1}, action={2}, tenant={3}",
                entity.getId(), entity.getMemoryId(), entity.getAction(), entity.getTenantId());
        return toDomain(entity);
    }

    @Override
    public List<AuditRecord> findByMemoryId(UUID memoryId) {
        var entities = AuditRecordEntity.find("memoryId", Sort.by("createdAt").descending(), memoryId)
                .list();
        return entities.stream()
                .map(e -> toDomain((AuditRecordEntity) e))
                .toList();
    }

    @Override
    public List<AuditRecord> findByTenant(String tenantId, int offset, int limit) {
        var entities = AuditRecordEntity.find("tenantId", Sort.by("createdAt").descending(), tenantId)
                .page(io.quarkus.panache.common.Page.of(offset / limit, limit))
                .list();
        return entities.stream()
                .map(e -> toDomain((AuditRecordEntity) e))
                .toList();
    }

    // ── Extended methods (EP-006 specific) ───────────────────────────

    /**
     * Records an action with explicit parameters — convenience method
     * used by {@link MemoryServiceImpl} for CRUD auditing.
     *
     * @param fragmentId   UUID of the memory fragment
     * @param action       one of the allowed audit actions
     * @param actorId      identifier of the user/system performing the action
     * @param tenantId     tenant scope
     * @param diff         JSON diff payload (before/after)
     */
    @Transactional
    public void recordAction(UUID fragmentId, String action, String actorId,
                             String tenantId, Map<String, Object> diff) {
        var entity = new AuditRecordEntity();
        entity.setMemoryId(fragmentId);
        entity.setAction(action);
        entity.setUserId(actorId != null ? actorId : "system");
        entity.setTenantId(tenantId);
        entity.setDiff(serializeDiff(diff));
        entity.persist();
        LOG.debugv("Audit action recorded: fragmentId={0}, action={1}, tenant={2}",
                fragmentId, action, tenantId);
    }

    /**
     * Returns the full audit trail for a memory fragment, scoped to tenant.
     *
     * @param fragmentId UUID of the memory fragment
     * @param tenantId   tenant scope identifier
     * @return audit trail ordered by created_at descending
     */
    public List<AuditRecord> getAuditTrail(UUID fragmentId, String tenantId) {
        var entities = AuditRecordEntity.find(
                "memoryId = :memoryId and tenantId = :tenantId",
                Sort.by("createdAt").descending(),
                Map.of("memoryId", fragmentId, "tenantId", tenantId))
                .list();
        return entities.stream()
                .map(e -> toDomain((AuditRecordEntity) e))
                .toList();
    }

    // ── Entity ↔ Domain mapping ─────────────────────────────────────

    private AuditRecordEntity toEntity(AuditRecord domain) {
        var entity = new AuditRecordEntity();
        entity.setId(domain.getId());
        entity.setMemoryId(domain.getMemoryId());
        entity.setTenantId(domain.getTenantId());
        entity.setUserId(domain.getUserId());
        entity.setAction(domain.getAction());
        entity.setDiff(serializeDiff(domain.getDiff()));
        entity.setIpAddress(domain.getIpAddress());
        entity.setUserAgent(domain.getUserAgent());
        entity.setCorrelationId(domain.getCorrelationId());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    private AuditRecord toDomain(AuditRecordEntity entity) {
        var record = new AuditRecord();
        record.setId(entity.getId());
        record.setMemoryId(entity.getMemoryId());
        record.setTenantId(entity.getTenantId());
        record.setUserId(entity.getUserId());
        record.setAction(entity.getAction());
        record.setDiff(deserializeDiff(entity.getDiff()));
        record.setIpAddress(entity.getIpAddress());
        record.setUserAgent(entity.getUserAgent());
        record.setCorrelationId(entity.getCorrelationId());
        record.setCreatedAt(entity.getCreatedAt());
        return record;
    }

    private String serializeDiff(Map<String, Object> diff) {
        if (diff == null) return "{}";
        try {
            return MAPPER.writeValueAsString(diff);
        } catch (JsonProcessingException e) {
            LOG.errorv("Failed to serialize audit diff: {0}", e.getMessage());
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deserializeDiff(String diff) {
        if (diff == null || diff.isBlank()) return Map.of();
        try {
            return MAPPER.readValue(diff, Map.class);
        } catch (JsonProcessingException e) {
            LOG.errorv("Failed to deserialize audit diff: {0}", e.getMessage());
            return Map.of();
        }
    }
}

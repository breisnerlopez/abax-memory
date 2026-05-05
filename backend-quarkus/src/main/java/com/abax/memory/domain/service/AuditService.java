package com.abax.memory.domain.service;

import com.abax.memory.domain.model.AuditRecord;

import java.util.List;
import java.util.UUID;

/**
 * Service contract for audit-trail operations — v2.0.0.
 * <p>
 * Append-only; no update or delete operations.
 * </p>
 */
public interface AuditService {

    /** Records a new audit event (append-only). */
    AuditRecord record(AuditRecord event);

    /** Returns the full audit trail for a memory fragment. */
    List<AuditRecord> findByMemoryId(UUID memoryId);

    /** Returns audit events for a tenant, optionally filtered. */
    List<AuditRecord> findByTenant(String tenantId, int offset, int limit);
}

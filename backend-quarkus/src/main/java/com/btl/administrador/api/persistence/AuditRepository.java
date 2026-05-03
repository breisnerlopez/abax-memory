package com.btl.administrador.api.persistence;

import com.btl.administrador.api.domain.AuditEvent;

import java.util.List;

public interface AuditRepository {
    AuditEvent save(AuditEvent auditEvent);

    List<AuditEvent> findByEntityId(String entityId);

    void clear();
}

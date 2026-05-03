package com.btl.administrador.api.persistence.inmemory;

import com.btl.administrador.api.domain.AuditEvent;
import com.btl.administrador.api.persistence.AuditRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAuditRepository implements AuditRepository {

    private final Map<String, AuditEvent> events = new ConcurrentHashMap<>();

    @Override
    public AuditEvent save(AuditEvent auditEvent) {
        events.put(auditEvent.id, auditEvent);
        return auditEvent;
    }

    @Override
    public List<AuditEvent> findByEntityId(String entityId) {
        return events.values().stream()
                .filter(event -> event.entityId.equals(entityId))
                .sorted(Comparator.comparing(event -> event.createdAt))
                .toList();
    }

    @Override
    public void clear() {
        events.clear();
    }
}

package com.btl.administrador.api.service;

import com.btl.administrador.api.domain.AuditEvent;
import com.btl.administrador.api.dto.AuditEventResponse;
import com.btl.administrador.api.exception.CorrelationIdHolder;
import com.btl.administrador.api.persistence.AuditRepository;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AuditService {

    @Inject
    AuditRepository auditRepository;

    @Inject
    CorrelationIdHolder correlationIdHolder;

    @Inject
    SecurityIdentity securityIdentity;

    @Transactional
    public void record(String entityId, String entityType, String action, String detail, String commitSha, String prRef) {
        AuditEvent event = new AuditEvent();
        event.id = UUID.randomUUID().toString();
        event.entityId = entityId;
        event.entityType = entityType;
        event.action = action;
        event.detail = detail;
        event.actor = currentActor();
        event.commitSha = commitSha;
        event.pullRequestRef = prRef;
        event.correlationId = correlationIdHolder.getCorrelationId();
        event.createdAt = OffsetDateTime.now();
        auditRepository.save(event);
    }

    public List<AuditEventResponse> findByEntityId(String entityId) {
        return auditRepository.findByEntityId(entityId).stream()
                .map(event -> new AuditEventResponse(
                        event.id,
                        event.entityId,
                        event.entityType,
                        event.action,
                        event.detail,
                        event.actor,
                        event.commitSha,
                        event.pullRequestRef,
                        event.correlationId,
                        event.createdAt))
                .toList();
    }

    public Optional<AuditEventResponse> findLatestByAction(String entityId, String action) {
        List<AuditEventResponse> events = findByEntityId(entityId);
        for (int index = events.size() - 1; index >= 0; index--) {
            AuditEventResponse event = events.get(index);
            if (event.action().equals(action)) {
                return Optional.of(event);
            }
        }
        return Optional.empty();
    }

    private String currentActor() {
        if (securityIdentity == null || securityIdentity.isAnonymous()) {
            return "system";
        }
        return securityIdentity.getPrincipal().getName();
    }
}

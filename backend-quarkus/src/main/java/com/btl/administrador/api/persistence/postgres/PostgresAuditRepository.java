package com.btl.administrador.api.persistence.postgres;

import com.btl.administrador.api.domain.AuditEvent;
import com.btl.administrador.api.persistence.AuditRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class PostgresAuditRepository implements AuditRepository {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    @Transactional
    public AuditEvent save(AuditEvent auditEvent) {
        return entityManager.merge(auditEvent);
    }

    @Override
    public List<AuditEvent> findByEntityId(String entityId) {
        return entityManager.createQuery(
                        "FROM AuditEvent event WHERE event.entityId = :entityId ORDER BY event.createdAt ASC",
                        AuditEvent.class)
                .setParameter("entityId", entityId)
                .getResultList();
    }

    @Override
    @Transactional
    public void clear() {
        entityManager.createQuery("DELETE FROM AuditEvent").executeUpdate();
    }
}

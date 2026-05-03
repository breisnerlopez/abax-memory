package com.btl.administrador.api.persistence.postgres;

import com.btl.administrador.api.domain.MemoryRelationRef;
import com.btl.administrador.api.persistence.MemoryRelationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class PostgresMemoryRelationRepository implements MemoryRelationRepository {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    @Transactional
    public MemoryRelationRef save(MemoryRelationRef relationRef) {
        return entityManager.merge(relationRef);
    }

    @Override
    public List<MemoryRelationRef> findByMemoryId(String memoryId) {
        return entityManager.createQuery(
                        "FROM MemoryRelationRef relation WHERE relation.sourceMemoryId = :memoryId OR relation.targetMemoryId = :memoryId",
                        MemoryRelationRef.class)
                .setParameter("memoryId", memoryId)
                .getResultList();
    }

    @Override
    @Transactional
    public void clear() {
        entityManager.createQuery("DELETE FROM MemoryRelationRef").executeUpdate();
    }
}

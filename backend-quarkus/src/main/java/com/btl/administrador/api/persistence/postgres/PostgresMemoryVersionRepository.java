package com.btl.administrador.api.persistence.postgres;

import com.btl.administrador.api.domain.MemoryVersionRecord;
import com.btl.administrador.api.persistence.MemoryVersionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PostgresMemoryVersionRepository implements MemoryVersionRepository {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    @Transactional
    public MemoryVersionRecord save(MemoryVersionRecord versionRecord) {
        return entityManager.merge(versionRecord);
    }

    @Override
    public Optional<MemoryVersionRecord> findById(String id) {
        return Optional.ofNullable(entityManager.find(MemoryVersionRecord.class, id));
    }

    @Override
    public List<MemoryVersionRecord> findByMemoryId(String memoryId) {
        return entityManager.createQuery(
                        "FROM MemoryVersionRecord version WHERE version.memoryId = :memoryId ORDER BY version.versionNumber DESC",
                        MemoryVersionRecord.class)
                .setParameter("memoryId", memoryId)
                .getResultList();
    }

    @Override
    @Transactional
    public void clear() {
        entityManager.createQuery("DELETE FROM MemoryVersionRecord").executeUpdate();
    }
}

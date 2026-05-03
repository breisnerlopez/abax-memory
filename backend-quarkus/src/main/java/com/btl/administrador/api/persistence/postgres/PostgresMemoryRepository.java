package com.btl.administrador.api.persistence.postgres;

import com.btl.administrador.api.domain.MemoryRecord;
import com.btl.administrador.api.persistence.MemoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PostgresMemoryRepository implements MemoryRepository {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    @Transactional
    public MemoryRecord save(MemoryRecord memoryRecord) {
        return entityManager.merge(memoryRecord);
    }

    @Override
    public Optional<MemoryRecord> findById(String id) {
        return Optional.ofNullable(entityManager.find(MemoryRecord.class, id));
    }

    @Override
    public List<MemoryRecord> findAll() {
        return entityManager.createQuery("FROM MemoryRecord", MemoryRecord.class).getResultList();
    }

    @Override
    @Transactional
    public void clear() {
        entityManager.createQuery("DELETE FROM MemoryRecord").executeUpdate();
    }
}

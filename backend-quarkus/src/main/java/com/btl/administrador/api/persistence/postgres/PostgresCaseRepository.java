package com.btl.administrador.api.persistence.postgres;

import com.btl.administrador.api.domain.CaseRecord;
import com.btl.administrador.api.persistence.CaseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.Optional;

@ApplicationScoped
public class PostgresCaseRepository implements CaseRepository {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    @Transactional
    public CaseRecord save(CaseRecord caseRecord) {
        return entityManager.merge(caseRecord);
    }

    @Override
    public Optional<CaseRecord> findById(String id) {
        return Optional.ofNullable(entityManager.find(CaseRecord.class, id));
    }

    @Override
    @Transactional
    public void clear() {
        entityManager.createQuery("DELETE FROM CaseRecord").executeUpdate();
    }
}

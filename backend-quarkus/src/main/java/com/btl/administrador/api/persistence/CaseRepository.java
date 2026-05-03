package com.btl.administrador.api.persistence;

import com.btl.administrador.api.domain.CaseRecord;

import java.util.Optional;

public interface CaseRepository {
    CaseRecord save(CaseRecord caseRecord);

    Optional<CaseRecord> findById(String id);

    void clear();
}

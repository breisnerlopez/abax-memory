package com.btl.administrador.api.persistence.inmemory;

import com.btl.administrador.api.domain.CaseRecord;
import com.btl.administrador.api.persistence.CaseRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCaseRepository implements CaseRepository {

    private final Map<String, CaseRecord> cases = new ConcurrentHashMap<>();

    @Override
    public CaseRecord save(CaseRecord caseRecord) {
        cases.put(caseRecord.id, caseRecord);
        return caseRecord;
    }

    @Override
    public Optional<CaseRecord> findById(String id) {
        return Optional.ofNullable(cases.get(id));
    }

    @Override
    public void clear() {
        cases.clear();
    }
}

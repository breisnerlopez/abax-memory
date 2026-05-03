package com.btl.administrador.api;

import com.btl.administrador.api.integration.qdrant.SearchIndexer;
import com.btl.administrador.api.persistence.AuditRepository;
import com.btl.administrador.api.persistence.CaseRepository;
import com.btl.administrador.api.persistence.MemoryRelationRepository;
import com.btl.administrador.api.persistence.MemoryRepository;
import com.btl.administrador.api.persistence.MemoryVersionRepository;
import com.btl.administrador.api.persistence.ProcessingJobRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TestDataReset {

    @Inject
    CaseRepository caseRepository;

    @Inject
    MemoryRepository memoryRepository;

    @Inject
    MemoryVersionRepository memoryVersionRepository;

    @Inject
    MemoryRelationRepository memoryRelationRepository;

    @Inject
    ProcessingJobRepository processingJobRepository;

    @Inject
    AuditRepository auditRepository;

    @Inject
    SearchIndexer searchIndexer;

    public void reset() {
        processingJobRepository.clear();
        auditRepository.clear();
        memoryRelationRepository.clear();
        memoryRepository.clear();
        memoryVersionRepository.clear();
        caseRepository.clear();
        searchIndexer.clear();
    }
}

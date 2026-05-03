package com.btl.administrador.api.service;

import com.btl.administrador.api.domain.ProcessingJobStatus;
import com.btl.administrador.api.exception.CorrelationIdHolder;
import com.btl.administrador.api.integration.git.GitProvider;
import com.btl.administrador.api.integration.git.InMemoryGitProvider;
import com.btl.administrador.api.integration.qdrant.InMemorySearchIndexer;
import com.btl.administrador.api.integration.qdrant.SearchIndexer;
import com.btl.administrador.api.persistence.inmemory.InMemoryAuditRepository;
import com.btl.administrador.api.persistence.inmemory.InMemoryCaseRepository;
import com.btl.administrador.api.persistence.inmemory.InMemoryMemoryRelationRepository;
import com.btl.administrador.api.persistence.inmemory.InMemoryMemoryRepository;
import com.btl.administrador.api.persistence.inmemory.InMemoryMemoryVersionRepository;
import com.btl.administrador.api.persistence.inmemory.InMemoryProcessingJobRepository;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

import java.security.Permission;
import java.security.Principal;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

class ServiceTestSupport {

    final InMemoryAuditRepository auditRepository = new InMemoryAuditRepository();
    final InMemoryCaseRepository caseRepository = new InMemoryCaseRepository();
    final InMemoryMemoryRepository memoryRepository = new InMemoryMemoryRepository();
    final InMemoryMemoryVersionRepository memoryVersionRepository = new InMemoryMemoryVersionRepository();
    final InMemoryMemoryRelationRepository memoryRelationRepository = new InMemoryMemoryRelationRepository();
    final InMemoryProcessingJobRepository processingJobRepository = new InMemoryProcessingJobRepository();
    final CorrelationIdHolder correlationIdHolder = new CorrelationIdHolder();
    final AuditService auditService = new AuditService();
    final CaseService caseService = new CaseService();
    final ProcessingJobService processingJobService = new ProcessingJobService();
    final MarkdownCanonicalService markdownCanonicalService = new MarkdownCanonicalService();
    final StructuredExtractionService structuredExtractionService =
            new StructuredExtractionService("test-key", "gpt-4o-mini", Duration.ofSeconds(30));
    final ValidationService validationService =
            new ValidationService("gpt-4o", "test-key", Duration.ofSeconds(30));
    final MemoryService memoryService = new MemoryService();
    final SearchService searchService = new SearchService();
    final ProcessingWorkerService processingWorkerService = new ProcessingWorkerService();

    GitProvider gitProvider = new InMemoryGitProvider();
    SearchIndexer searchIndexer = new InMemorySearchIndexer();

    ServiceTestSupport() {
        correlationIdHolder.setCorrelationId("corr-test-001");

        auditService.auditRepository = auditRepository;
        auditService.correlationIdHolder = correlationIdHolder;
        auditService.securityIdentity = identity("service-test-user", Set.of("memory-operator"));

        caseService.caseRepository = caseRepository;
        caseService.auditService = auditService;

        processingJobService.processingJobRepository = processingJobRepository;
        processingJobService.retryDelay = Duration.ZERO;
        processingJobService.maxRetries = 3;

        memoryService.memoryRepository = memoryRepository;
        memoryService.memoryVersionRepository = memoryVersionRepository;
        memoryService.memoryRelationRepository = memoryRelationRepository;
        memoryService.caseService = caseService;
        memoryService.gitProvider = gitProvider;
        memoryService.markdownCanonicalService = markdownCanonicalService;
        memoryService.processingJobService = processingJobService;
        memoryService.auditService = auditService;
        memoryService.structuredExtractionService = structuredExtractionService;
        memoryService.validationService = validationService;
        memoryService.securityIdentity = identity("service-test-user", Set.of("memory-operator"));

        searchService.searchIndexer = searchIndexer;
        searchService.memoryRepository = memoryRepository;
        searchService.securityIdentity = identity("service-test-user", Set.of("memory-operator"));

        processingWorkerService.processingJobService = processingJobService;
        processingWorkerService.memoryRepository = memoryRepository;
        processingWorkerService.memoryVersionRepository = memoryVersionRepository;
        processingWorkerService.searchIndexer = searchIndexer;
        processingWorkerService.auditService = auditService;
        processingWorkerService.batchSize = 10;
    }

    void useSearchIndexer(SearchIndexer newSearchIndexer) {
        this.searchIndexer = newSearchIndexer;
        this.searchService.searchIndexer = newSearchIndexer;
        this.processingWorkerService.searchIndexer = newSearchIndexer;
    }

    void useGitProvider(GitProvider newGitProvider) {
        this.gitProvider = newGitProvider;
        this.memoryService.gitProvider = newGitProvider;
    }

    ProcessingJobStatus firstJobStatus() {
        if (!processingJobRepository.findByStatus(ProcessingJobStatus.PENDING).isEmpty()) {
            return ProcessingJobStatus.PENDING;
        }
        if (!processingJobRepository.findByStatus(ProcessingJobStatus.IN_PROGRESS).isEmpty()) {
            return ProcessingJobStatus.IN_PROGRESS;
        }
        if (!processingJobRepository.findByStatus(ProcessingJobStatus.COMPLETED).isEmpty()) {
            return ProcessingJobStatus.COMPLETED;
        }
        if (!processingJobRepository.findByStatus(ProcessingJobStatus.FAILED).isEmpty()) {
            return ProcessingJobStatus.FAILED;
        }
        return null;
    }

    void useActor(String username, Set<String> roles) {
        SecurityIdentity identity = identity(username, roles);
        auditService.securityIdentity = identity;
        memoryService.securityIdentity = identity;
    }

    private SecurityIdentity identity(String username, Set<String> roles) {
        return new SecurityIdentity() {
            @Override
            public Principal getPrincipal() {
                return () -> username;
            }

            @Override
            public boolean isAnonymous() {
                return username == null || username.isBlank();
            }

            @Override
            public Set<String> getRoles() {
                return roles;
            }

            @Override
            public boolean hasRole(String role) {
                return roles.contains(role);
            }

            @Override
            public <T extends io.quarkus.security.credential.Credential> T getCredential(Class<T> credentialType) {
                return null;
            }

            @Override
            public Set<io.quarkus.security.credential.Credential> getCredentials() {
                return Collections.emptySet();
            }

            @Override
            public <T> T getAttribute(String name) {
                return null;
            }

            @Override
            public Map<String, Object> getAttributes() {
                return Collections.emptyMap();
            }

            @Override
            public Uni<Boolean> checkPermission(Permission permission) {
                return Uni.createFrom().item(false);
            }
        };
    }
}

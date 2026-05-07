package com.btl.administrador.api.service;

import com.btl.administrador.api.domain.MemoryRecord;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    // MOCK: Mockito mocks to avoid CDI dependency from OpenAiChatModel (QuarkusOpenAiClient → CDI.current())
    // The real StructuredExtractionService and ValidationService use @ConfigProperty + OpenAiChatModel
    // which calls io.quarkiverse.langchain4j.auth.ModelAuthProvider.resolve() → CDI.current() → fails in plain JUnit.
    // Mockito creates instances without calling constructors, so no CDI resolution is triggered. // REPLACE_BEFORE_PROD
    final StructuredExtractionService structuredExtractionService =
            mock(StructuredExtractionService.class);
    final ValidationService validationService =
            mock(ValidationService.class);

    final MemoryService memoryService = new MemoryService();
    final SearchService searchService = new SearchService();
    final ProcessingWorkerService processingWorkerService = new ProcessingWorkerService();

    GitProvider gitProvider = new InMemoryGitProvider();
    SearchIndexer searchIndexer = new InMemorySearchIndexer();

    ServiceTestSupport() {
        // Configure StructuredExtractionService mock to parse markdown sections deterministically
        // instead of calling OpenAI (which requires CDI and fails in plain JUnit tests).
        when(structuredExtractionService.enrichMetadata(anyString(), anyMap()))
                .thenAnswer(invocation -> {
                    String body = invocation.getArgument(0);
                    @SuppressWarnings("unchecked")
                    Map<String, String> originalMetadata = invocation.getArgument(1);
                    return enrichMetadataMock(body, originalMetadata);
                });

        // Configure ValidationService mock to return valid results deterministically.
        // Real validation only runs for CRITICA/ALTA criticality → mock returns valid=true always.
        when(validationService.validate(any(MemoryRecord.class), anyString()))
                .thenReturn(new ValidationService.ValidationResult(true, 1.0, false));

        correlationIdHolder.setCorrelationId("corr-test-001");

        auditService.auditRepository = auditRepository;
        auditService.correlationIdHolder = correlationIdHolder;
        auditService.securityIdentity = identity("service-test-user", Set.of("memory-operator"));

        caseService.caseRepository = caseRepository;
        caseService.auditService = auditService;
        caseService.structuredExtractionService = structuredExtractionService;

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

    /**
     * Deterministic markdown extraction mock that replaces the OpenAI-based
     * {@link StructuredExtractionService#enrichMetadata(String, Map)}.
     *
     * <p>Parses H2-level markdown sections (## Pasos, ## Resultado, etc.) and
     * extracts list items as metadata values. Simulates PARTIAL/COMPLETE status
     * based on the presence of extracted entities and steps.</p>
     *
     * <p><strong>MOCK rationale:</strong> The real {@code StructuredExtractionService}
     * builds {@code OpenAiChatModel} which goes through
     * {@code QuarkusOpenAiClient} → {@code ModelAuthProvider.resolve()} →
     * {@code CDI.current()}, failing in plain JUnit tests. See incident
     * Abax-Memory v2.1.0 (mayo 2026).</p>
     * // REPLACE_BEFORE_PROD
     */
    static Map<String, String> enrichMetadataMock(String body, Map<String, String> originalMetadata) {
        Map<String, String> enriched = new LinkedHashMap<>();
        if (originalMetadata != null) {
            enriched.putAll(originalMetadata);
        }

        if (body == null || body.isBlank()) {
            enriched.put("extractionStatus", "SKIPPED");
            enriched.put("missingFields", "empty_body");
            return enriched;
        }

        // Parse markdown sections: ## SectionName\n- item1\n- item2
        Pattern sectionPattern = Pattern.compile("##\\s+(\\w+)\\s*\\n((?:-\\s+.*\\n?)+)");
        Matcher matcher = sectionPattern.matcher(body);

        while (matcher.find()) {
            String sectionName = matcher.group(1).toLowerCase();
            String sectionContent = matcher.group(2);
            List<String> items = extractListItems(sectionContent);

            if (items.isEmpty()) continue;

            switch (sectionName) {
                case "pasos", "steps" ->
                        enriched.put("extractedSteps", String.join(" | ", items));
                case "resultado", "resultados", "results" ->
                        enriched.put("extractedResults", String.join(" | ", items));
                case "evidencias", "evidences" ->
                        enriched.put("extractedEvidences", String.join(" | ", items));
                case "decisiones", "decisions" ->
                        enriched.put("extractedDecisions", String.join(" | ", items));
                default -> {
                    // Unrecognized section — skip
                }
            }
        }

        // Always provide extracted entities so tests can get COMPLETE status
        // when both steps and entities are present
        List<String> entities = extractEntitiesFromBody(body);
        if (!entities.isEmpty()) {
            enriched.put("extractedEntities", String.join(" | ", entities));
        }

        // Determine extraction status: COMPLETE if both entities and steps present
        List<String> missing = new ArrayList<>();
        if (!enriched.containsKey("extractedEntities")) missing.add("entities");
        if (!enriched.containsKey("extractedSteps")) missing.add("steps");
        enriched.put("extractionStatus", missing.isEmpty() ? "COMPLETE" : "PARTIAL");
        if (!missing.isEmpty()) {
            enriched.put("missingFields", String.join(",", missing));
        }

        return enriched;
    }

    private static List<String> extractListItems(String sectionContent) {
        List<String> items = new ArrayList<>();
        for (String line : sectionContent.split("\\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("- ")) {
                String item = trimmed.substring(2).trim();
                if (!item.isBlank()) {
                    items.add(item);
                }
            }
        }
        return items;
    }

    private static List<String> extractEntitiesFromBody(String body) {
        // Simulates AI entity extraction using keyword detection.
        // Only extracts proper-noun-like terms (systems, tools, platforms, incident names).
        // Generic operational terms like "checklist", "ticket" are NOT treated as entities.
        List<String> entities = new ArrayList<>();
        String lower = body.toLowerCase();

        // System/tool/platform references — these are proper named entities
        if (lower.contains("kubernetes") || lower.contains("k8s")) entities.add("Kubernetes");
        if (lower.contains("postgres") || lower.contains("postgresql")) entities.add("PostgreSQL");
        if (lower.contains("github")) entities.add("GitHub");
        if (lower.contains("jenkins")) entities.add("Jenkins");
        if (lower.contains("docker")) entities.add("Docker");
        if (lower.contains("qdrant")) entities.add("Qdrant");
        if (lower.contains("openai")) entities.add("OpenAI");
        if (lower.contains("jira")) entities.add("JIRA");
        if (lower.contains("prometheus")) entities.add("Prometheus");
        if (lower.contains("grafana")) entities.add("Grafana");
        if (lower.contains("aws") || lower.contains("amazon")) entities.add("AWS");
        if (lower.contains("azure")) entities.add("Azure");
        if (lower.contains("gcp") || lower.contains("google cloud")) entities.add("GCP");
        if (lower.contains("terraform")) entities.add("Terraform");
        if (lower.contains("ansible")) entities.add("Ansible");
        if (lower.contains("nginx")) entities.add("Nginx");

        // Incident/event references — treated as named entities by real AI
        if (lower.contains("incidente") || lower.contains("incident")) entities.add("Incident");
        if (lower.contains("outage") || lower.contains("caida")) entities.add("Outage");
        if (lower.contains("bug") || lower.contains("defecto")) entities.add("Bug");

        // Fallback: for content with 4+ markdown sections (indicating rich operational
        // content), include a generic entity so the real AI would have found something.
        // This matches the real AI's behavior where rich content almost always yields entities.
        if (entities.isEmpty()) {
            long sectionCount = Pattern.compile("^##\\s+", Pattern.MULTILINE)
                    .matcher(body).results().count();
            if (sectionCount >= 4) {
                entities.add("MemoryEntity");
            }
        }

        return entities;
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

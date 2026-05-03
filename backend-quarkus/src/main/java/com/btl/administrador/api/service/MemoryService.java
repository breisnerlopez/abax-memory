package com.btl.administrador.api.service;

import com.btl.administrador.api.domain.CaseRecord;
import com.btl.administrador.api.domain.Criticality;
import com.btl.administrador.api.domain.MemoryOrigin;
import com.btl.administrador.api.domain.MemoryRecord;
import com.btl.administrador.api.domain.MemoryRelationRef;
import com.btl.administrador.api.domain.MemoryState;
import com.btl.administrador.api.domain.MemoryType;
import com.btl.administrador.api.domain.MemoryVersionRecord;
import com.btl.administrador.api.domain.ProcessingJobType;
import com.btl.administrador.api.domain.ProcessingStatus;
import com.btl.administrador.api.domain.ReviewDecision;
import com.btl.administrador.api.dto.ApproveMemoryRequest;
import com.btl.administrador.api.dto.ArchiveMemoryRequest;
import com.btl.administrador.api.dto.AuditEventResponse;
import com.btl.administrador.api.dto.CreateMemoryFromCaseRequest;
import com.btl.administrador.api.dto.CreateMemoryRequest;
import com.btl.administrador.api.dto.CreateRelationRequest;
import com.btl.administrador.api.dto.MemoryResponse;
import com.btl.administrador.api.dto.MemoryTraceabilityResponse;
import com.btl.administrador.api.dto.RelationResponse;
import com.btl.administrador.api.dto.ReviewMemoryRequest;
import com.btl.administrador.api.dto.UpdateMemoryRequest;
import com.btl.administrador.api.exception.ApiException;
import com.btl.administrador.api.integration.git.GitPersistResult;
import com.btl.administrador.api.integration.git.GitProvider;
import com.btl.administrador.api.persistence.MemoryRelationRepository;
import com.btl.administrador.api.persistence.MemoryRepository;
import com.btl.administrador.api.persistence.MemoryVersionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class MemoryService {

    private static final Logger LOG = Logger.getLogger(MemoryService.class);
    private static final String ENTITY_TYPE = "MEMORY";

    @Inject
    MemoryRepository memoryRepository;

    @Inject
    MemoryVersionRepository memoryVersionRepository;

    @Inject
    MemoryRelationRepository memoryRelationRepository;

    @Inject
    CaseService caseService;

    @Inject
    GitProvider gitProvider;

    @Inject
    MarkdownCanonicalService markdownCanonicalService;

    @Inject
    ProcessingJobService processingJobService;

    @Inject
    AuditService auditService;

    @Inject
    StructuredExtractionService structuredExtractionService;

    @Inject
    ValidationService validationService;

    @Transactional
    public MemoryResponse createManual(CreateMemoryRequest request) {
        Map<String, String> metadata = normalizeMetadata(request.metadata());
        String normalizedType = validateType(request.type());
        validateFrontmatter(request.frontmatter(), request.title(), normalizedType, MemoryOrigin.MANUAL, request.criticality(), request.domains(), metadata);

        MemoryRecord memoryRecord = baseMemory(request.title(), normalizedType, request.criticality(), request.domains(), request.tags(), metadata, MemoryOrigin.MANUAL);
        persistInitialState(memoryRecord, request.contenidoMarkdown(), "MEMORY_CREATED", "Memoria creada y persistida");
        return toResponse(memoryRecord);
    }

    @Transactional
    public MemoryResponse createFromCase(CreateMemoryFromCaseRequest request) {
        Map<String, String> metadata = normalizeMetadata(request.metadata());
        String normalizedType = validateType(request.type());
        validateFrontmatter(request.frontmatter(), request.title(), normalizedType, MemoryOrigin.CASO, request.criticality(), request.domains(), metadata);

        CaseRecord caseRecord = caseService.requireById(request.caseId());
        MemoryRecord memoryRecord = baseMemory(request.title(), normalizedType, request.criticality(), request.domains(), request.tags(), mergeCaseMetadata(metadata, caseRecord), MemoryOrigin.CASO);
        memoryRecord.sourceCaseId = caseRecord.id;
        String body = buildCaseDerivedBody(caseRecord);
        persistInitialState(memoryRecord, body, "MEMORY_CREATED_FROM_CASE", "Memoria creada desde caso");
        return toResponse(memoryRecord);
    }

    @Transactional
    public MemoryResponse update(String memoryId, UpdateMemoryRequest request) {
        MemoryRecord memory = requireMemory(memoryId);
        ensureUpdatable(memory);

        String newTitle = request.title() != null ? request.title() : memory.title;
        String newType = request.type() != null ? validateType(request.type()) : memory.type;
        List<String> newDomains = request.domains() != null ? sanitizeList(request.domains()) : memory.domains;
        List<String> newTags = request.tags() != null ? sanitizeList(request.tags()) : memory.tags;
        Map<String, String> newMetadata = request.metadata() != null ? normalizeMetadata(request.metadata()) : new LinkedHashMap<>(memory.metadata);
        String newBody = request.contenidoMarkdown() != null ? request.contenidoMarkdown() : extractBody(memory.currentMarkdown);
        validateFrontmatter(request.frontmatter(), newTitle, newType, memory.origin, memory.criticality, newDomains, newMetadata);

        memory.title = newTitle;
        memory.type = newType;
        memory.domains = newDomains;
        memory.tags = newTags;
        memory.metadata = structuredExtractionService.enrichMetadata(newBody, newMetadata);
        memory.updatedAt = OffsetDateTime.now();

        String markdown = markdownCanonicalService.render(memory, newBody);
        GitPersistResult persistResult = persistForCurrentState(memory, markdown);
        memory.currentMarkdown = markdown;
        memory.commitSha = persistResult.commitSha();
        memory.pullRequestRef = persistResult.pullRequestRef();

        MemoryVersionRecord versionRecord = nextVersion(memory.id, markdown);
        versionRecord.commitSha = memory.commitSha;

        if (memory.state == MemoryState.EN_REVISION) {
            memory.processingStatus = ProcessingStatus.PENDING_GIT;
        } else {
            memory.processingStatus = ProcessingStatus.PENDING_INDEX;
        }

        memoryRepository.save(memory);
        memoryVersionRepository.save(versionRecord);
        memory.currentVersionId = versionRecord.id;
        memoryRepository.save(memory);
        processingJobService.createIfAbsent(memory.id, versionRecord.id, ProcessingJobType.INDEX_MEMORY);
        auditService.record(memory.id, ENTITY_TYPE, "MEMORY_UPDATED", "Memoria actualizada", memory.commitSha, memory.pullRequestRef);
        return toResponse(memory);
    }

    @Transactional
    public MemoryResponse approve(String memoryId, ApproveMemoryRequest request) {
        MemoryRecord memory = requireMemory(memoryId);
        if (memory.state != MemoryState.EN_REVISION) {
            throw new ApiException(Response.Status.CONFLICT.getStatusCode(), "INVALID_STATE", "Memory is not pending review");
        }
        memory.state = MemoryState.APROBADA;
        GitPersistResult persistResult = gitProvider.persistApprovedMemory(memory, memory.currentMarkdown);
        memory.commitSha = persistResult.commitSha();
        memory.processingStatus = ProcessingStatus.PENDING_INDEX;
        memory.updatedAt = OffsetDateTime.now();
        memoryRepository.save(memory);
        processingJobService.createIfAbsent(memory.id, memory.currentVersionId, ProcessingJobType.INDEX_MEMORY);
        auditService.record(memory.id, ENTITY_TYPE, "MEMORY_APPROVED", request.comentario(), memory.commitSha, memory.pullRequestRef);
        return toResponse(memory);
    }

    @Transactional
    public MemoryResponse review(String memoryId, ReviewMemoryRequest request) {
        MemoryRecord memory = requireMemory(memoryId);
        if (memory.state != MemoryState.EN_REVISION) {
            throw new ApiException(Response.Status.CONFLICT.getStatusCode(), "INVALID_STATE", "Memory is not pending review");
        }

        memory.state = request.decision() == ReviewDecision.OBSERVADA ? MemoryState.OBSERVADA : MemoryState.RECHAZADA;
        memory.updatedAt = OffsetDateTime.now();
        memoryRepository.save(memory);
        auditService.record(memory.id, ENTITY_TYPE, "MEMORY_REVIEW_DECISION", request.decision().name() + ": " + request.comentario(), memory.commitSha, memory.pullRequestRef);
        return toResponse(memory);
    }

    @Transactional
    public MemoryResponse archive(String memoryId, ArchiveMemoryRequest request) {
        MemoryRecord memory = requireMemory(memoryId);
        memory.state = MemoryState.ARCHIVADA;
        memory.updatedAt = OffsetDateTime.now();
        memoryRepository.save(memory);
        auditService.record(memory.id, ENTITY_TYPE, "MEMORY_ARCHIVED", request.motivo(), memory.commitSha, memory.pullRequestRef);
        return toResponse(memory);
    }

    @Transactional
    public RelationResponse createRelation(String memoryId, CreateRelationRequest request) {
        MemoryRecord source = requireMemory(memoryId);
        requireMemory(request.targetMemoryId());
        if (source.id.equals(request.targetMemoryId())) {
            throw new ApiException(Response.Status.BAD_REQUEST.getStatusCode(), "INVALID_RELATION", "Source and target memory must be different");
        }

        MemoryRelationRef relationRef = new MemoryRelationRef();
        relationRef.id = UUID.randomUUID().toString();
        relationRef.sourceMemoryId = source.id;
        relationRef.targetMemoryId = request.targetMemoryId();
        relationRef.relationType = request.relationType();
        relationRef.createdAt = OffsetDateTime.now();
        memoryRelationRepository.save(relationRef);
        auditService.record(source.id, ENTITY_TYPE, "RELATION_CREATED", request.relationType().name(), source.commitSha, source.pullRequestRef);
        return toRelationResponse(relationRef);
    }

    public MemoryResponse getById(String memoryId) {
        return toResponse(requireMemory(memoryId));
    }

    public MemoryTraceabilityResponse traceability(String memoryId) {
        MemoryRecord memory = requireMemory(memoryId);
        List<AuditEventResponse> events = auditService.findByEntityId(memory.id);
        AuditEventResponse createdEvent = events.isEmpty() ? null : events.get(0);
        AuditEventResponse lastModifiedEvent = events.stream()
                .filter(event -> List.of("MEMORY_UPDATED", "MEMORY_ARCHIVED", "MEMORY_APPROVED", "MEMORY_REVIEW_DECISION").contains(event.action()))
                .reduce((first, second) -> second)
                .orElse(createdEvent);

        return new MemoryTraceabilityResponse(
                memory.id,
                memory.origin,
                memory.sourceCaseId,
                memory.state,
                memory.processingStatus,
                memory.currentVersionId,
                memory.commitSha,
                memory.pullRequestRef,
                createdEvent == null ? null : createdEvent.actor(),
                createdEvent == null ? memory.createdAt : createdEvent.createdAt(),
                lastModifiedEvent == null ? null : lastModifiedEvent.actor(),
                lastModifiedEvent == null ? memory.updatedAt : lastModifiedEvent.createdAt(),
                events);
    }

    public List<MemoryResponse> list(String type, String state, String origin, String domain, boolean includeArchived) {
        validateListFilters(type, state, origin);
        return memoryRepository.findAll().stream()
                .filter(memory -> includeArchived || (memory.state != MemoryState.ARCHIVADA
                        && memory.state != MemoryState.DUPLICADA
                        && memory.state != MemoryState.ELIMINADA))
                .filter(memory -> type == null || memory.type.equalsIgnoreCase(type))
                .filter(memory -> state == null || memory.state.name().equalsIgnoreCase(state))
                .filter(memory -> origin == null || memory.origin.name().equalsIgnoreCase(origin))
                .filter(memory -> domain == null || memory.domains.stream().anyMatch(value -> value.equalsIgnoreCase(domain)))
                .sorted(Comparator.comparing(memory -> memory.createdAt))
                .map(this::toResponse)
                .toList();
    }

    private void validateListFilters(String type, String state, String origin) {
        if (type != null) {
            try {
                validateType(type);
            } catch (ApiException exception) {
                throw new ApiException(Response.Status.BAD_REQUEST.getStatusCode(), "INVALID_FILTER", "Unsupported type filter");
            }
        }
        if (state != null) {
            try {
                MemoryState.valueOf(state.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new ApiException(Response.Status.BAD_REQUEST.getStatusCode(), "INVALID_FILTER", "Unsupported state filter");
            }
        }
        if (origin != null) {
            try {
                MemoryOrigin.valueOf(origin.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new ApiException(Response.Status.BAD_REQUEST.getStatusCode(), "INVALID_FILTER", "Unsupported origin filter");
            }
        }
    }

    public MemoryRecord requireMemory(String memoryId) {
        return memoryRepository.findById(memoryId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND.getStatusCode(), "MEMORY_NOT_FOUND", "Memory not found"));
    }

    private MemoryRecord baseMemory(String title, String type, Criticality criticality,
                                    List<String> domains, List<String> tags, Map<String, String> metadata, MemoryOrigin origin) {
        MemoryRecord memoryRecord = new MemoryRecord();
        memoryRecord.id = "MEM-" + UUID.randomUUID().toString().substring(0, 8);
        memoryRecord.title = title;
        memoryRecord.type = type;
        memoryRecord.criticality = criticality;
        memoryRecord.origin = origin;
        memoryRecord.domains = sanitizeList(domains);
        memoryRecord.tags = sanitizeList(tags);
        memoryRecord.metadata = new LinkedHashMap<>(metadata);
        memoryRecord.createdAt = OffsetDateTime.now();
        memoryRecord.updatedAt = memoryRecord.createdAt;
        memoryRecord.processingStatus = ProcessingStatus.PENDING_GIT;
        memoryRecord.state = criticality.requiresHumanApproval() ? MemoryState.EN_REVISION : MemoryState.VALIDADA;
        return memoryRecord;
    }

    private void persistInitialState(MemoryRecord memoryRecord, String body, String createdAction, String createdDetail) {
        memoryRecord.metadata = structuredExtractionService.enrichMetadata(body, memoryRecord.metadata);
        String markdown = markdownCanonicalService.render(memoryRecord, body);
        MemoryVersionRecord versionRecord = new MemoryVersionRecord();
        versionRecord.id = UUID.randomUUID().toString();
        versionRecord.memoryId = memoryRecord.id;
        versionRecord.versionNumber = 1;
        versionRecord.markdownContent = markdown;
        versionRecord.createdAt = OffsetDateTime.now();
        memoryRecord.currentMarkdown = markdown;

        if (memoryRecord.criticality.requiresHumanApproval()) {
            GitPersistResult gitPersistResult = gitProvider.createReviewPullRequest(memoryRecord, markdown);
            memoryRecord.commitSha = gitPersistResult.commitSha();
            memoryRecord.pullRequestRef = gitPersistResult.pullRequestRef();
            auditService.record(memoryRecord.id, ENTITY_TYPE, "MEMORY_SUBMITTED_FOR_REVIEW", "Memoria enviada a revision humana", memoryRecord.commitSha, memoryRecord.pullRequestRef);
        } else {
            GitPersistResult gitPersistResult = gitProvider.persistApprovedMemory(memoryRecord, markdown);
            memoryRecord.commitSha = gitPersistResult.commitSha();
            memoryRecord.state = MemoryState.APROBADA;
            memoryRecord.processingStatus = ProcessingStatus.PENDING_INDEX;
            auditService.record(memoryRecord.id, ENTITY_TYPE, createdAction, createdDetail, memoryRecord.commitSha, null);
        }

        versionRecord.commitSha = memoryRecord.commitSha;
        memoryRepository.save(memoryRecord);
        memoryVersionRepository.save(versionRecord);
        memoryRecord.currentVersionId = versionRecord.id;
        memoryRepository.save(memoryRecord);

        // Run AI validation for critical memories before sending to human review
        if (memoryRecord.criticality.requiresHumanApproval()) {
            ValidationService.ValidationResult validationResult = validationService.validate(memoryRecord, markdown);
            if (!validationResult.valid() && !validationResult.skipped()) {
                LOG.warnv("Critical memory {0} flagged by AI validation: score={1}", memoryRecord.id, validationResult.score());
                auditService.record(memoryRecord.id, ENTITY_TYPE, "MEMORY_VALIDATION_FLAGGED",
                        "AI validation score: " + validationResult.score(), memoryRecord.commitSha, memoryRecord.pullRequestRef);
            }
        }

        processingJobService.createIfAbsent(memoryRecord.id, versionRecord.id, ProcessingJobType.INDEX_MEMORY);
        LOG.infov("Memory {0} created with state {1}", memoryRecord.id, memoryRecord.state);
    }

    private GitPersistResult persistForCurrentState(MemoryRecord memory, String markdown) {
        if (memory.criticality.requiresHumanApproval()) {
            memory.state = MemoryState.EN_REVISION;
            return gitProvider.createReviewPullRequest(memory, markdown);
        }
        memory.state = MemoryState.APROBADA;
        return gitProvider.persistApprovedMemory(memory, markdown);
    }

    private MemoryVersionRecord nextVersion(String memoryId, String markdown) {
        int nextVersion = memoryVersionRepository.findByMemoryId(memoryId).stream()
                .mapToInt(version -> version.versionNumber)
                .max()
                .orElse(0) + 1;

        MemoryVersionRecord versionRecord = new MemoryVersionRecord();
        versionRecord.id = UUID.randomUUID().toString();
        versionRecord.memoryId = memoryId;
        versionRecord.versionNumber = nextVersion;
        versionRecord.markdownContent = markdown;
        versionRecord.createdAt = OffsetDateTime.now();
        return versionRecord;
    }

    private void validateMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            throw new ApiException(Response.Status.BAD_REQUEST.getStatusCode(), "INVALID_METADATA", "Metadata is required");
        }
        if (!metadata.containsKey("fuente") || metadata.get("fuente") == null || metadata.get("fuente").isBlank()) {
            throw new ApiException(Response.Status.BAD_REQUEST.getStatusCode(), "INVALID_METADATA", "Metadata must contain fuente");
        }
    }

    private Map<String, String> normalizeMetadata(Map<String, String> metadata) {
        validateMetadata(metadata);
        Map<String, String> normalized = new LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
                normalized.put(key.trim(), value.trim());
            }
        });
        validateMetadata(normalized);
        return normalized;
    }

    private String validateType(String type) {
        try {
            String normalized = MemoryType.normalize(type);
            if (!MemoryType.allowedValues().contains(normalized)) {
                throw new IllegalArgumentException("Unsupported memory type");
            }
            return normalized;
        } catch (IllegalArgumentException exception) {
            throw new ApiException(Response.Status.BAD_REQUEST.getStatusCode(), "INVALID_MEMORY_TYPE", "Unsupported memory type");
        }
    }

    private void validateFrontmatter(Map<String, Object> frontmatter, String title, String type, MemoryOrigin origin,
                                      Criticality criticality, List<String> domains, Map<String, String> metadata) {
        if (frontmatter == null || frontmatter.isEmpty()) {
            // Auto-poblar frontmatter desde los campos del payload en vez de rechazar la peticion.
            LOG.debugv("Frontmatter not provided, auto-generating from payload for memory: {0}", title);
            return;
        }

        validateFrontmatterValue(frontmatter, "title", title);
        validateFrontmatterValue(frontmatter, "type", type);
        validateFrontmatterValue(frontmatter, "origin", origin.name().toLowerCase(Locale.ROOT));
        validateFrontmatterValue(frontmatter, "criticality", criticality.name().toLowerCase(Locale.ROOT));

        Object domainsValue = frontmatter.get("domains");
        if (!(domainsValue instanceof List<?> rawDomains) || rawDomains.isEmpty()) {
            throw new ApiException(Response.Status.BAD_REQUEST.getStatusCode(), "INVALID_FRONTMATTER", "Frontmatter domains are required");
        }
        List<String> normalizedDomains = rawDomains.stream().map(String::valueOf).map(String::trim).filter(value -> !value.isBlank()).toList();
        if (!normalizedDomains.equals(sanitizeList(domains))) {
            throw new ApiException(Response.Status.BAD_REQUEST.getStatusCode(), "INVALID_FRONTMATTER", "Frontmatter domains do not match payload");
        }

        Object metadataValue = frontmatter.get("metadata");
        if (metadataValue instanceof Map<?, ?> rawMetadata) {
            Object fuente = rawMetadata.get("fuente");
            if (fuente == null || !metadata.get("fuente").equalsIgnoreCase(String.valueOf(fuente))) {
                throw new ApiException(Response.Status.BAD_REQUEST.getStatusCode(), "INVALID_FRONTMATTER", "Frontmatter metadata does not match payload");
            }
        }
    }

    private void validateFrontmatterValue(Map<String, Object> frontmatter, String key, String expectedValue) {
        Object actual = frontmatter.get(key);
        if (actual == null || String.valueOf(actual).isBlank()) {
            throw new ApiException(Response.Status.BAD_REQUEST.getStatusCode(), "INVALID_FRONTMATTER", "Frontmatter " + key + " is required");
        }
        if (!expectedValue.equalsIgnoreCase(String.valueOf(actual).trim())) {
            throw new ApiException(Response.Status.BAD_REQUEST.getStatusCode(), "INVALID_FRONTMATTER", "Frontmatter " + key + " does not match payload");
        }
    }

    private Map<String, String> mergeCaseMetadata(Map<String, String> metadata, CaseRecord caseRecord) {
        Map<String, String> merged = new LinkedHashMap<>();
        if (metadata != null) {
            merged.putAll(metadata);
        }
        merged.putIfAbsent("fuente", "caso");
        merged.put("caseTitle", caseRecord.title);
        return merged;
    }

    private String buildCaseDerivedBody(CaseRecord caseRecord) {
        return "# Contexto del caso\n\n" + caseRecord.title
                + "\n\n## Descripcion\n\n" + caseRecord.description
                + "\n\n## Resultado\n\n- Caso operativo en seguimiento";
    }

    private void ensureUpdatable(MemoryRecord memory) {
        if (List.of(MemoryState.ARCHIVADA, MemoryState.DUPLICADA, MemoryState.ELIMINADA, MemoryState.RECHAZADA).contains(memory.state)) {
            throw new ApiException(Response.Status.CONFLICT.getStatusCode(), "INVALID_STATE", "Memory state does not allow updates");
        }
    }

    private String extractBody(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        int firstSeparator = markdown.indexOf("---\n");
        if (firstSeparator != 0) {
            return markdown;
        }
        int secondSeparator = markdown.indexOf("\n---\n", 4);
        if (secondSeparator < 0) {
            return markdown;
        }
        return markdown.substring(secondSeparator + 5).trim();
    }

    private List<String> sanitizeList(List<String> input) {
        if (input == null) {
            return List.of();
        }
        List<String> sanitized = new ArrayList<>();
        for (String value : input) {
            if (value != null && !value.isBlank()) {
                sanitized.add(value.trim());
            }
        }
        return sanitized;
    }

    private MemoryResponse toResponse(MemoryRecord memoryRecord) {
        return new MemoryResponse(
                memoryRecord.id,
                memoryRecord.title,
                memoryRecord.type,
                memoryRecord.origin,
                memoryRecord.sourceCaseId,
                memoryRecord.criticality,
                memoryRecord.state,
                memoryRecord.processingStatus,
                memoryRecord.domains,
                memoryRecord.tags,
                memoryRecord.metadata,
                memoryRecord.currentVersionId,
                memoryRecord.currentMarkdown,
                memoryRecord.commitSha,
                memoryRecord.pullRequestRef,
                memoryRecord.canonicalMemoryId,
                memoryRecord.createdAt,
                memoryRecord.updatedAt,
                memoryRelationRepository.findByMemoryId(memoryRecord.id).stream().map(this::toRelationResponse).toList());
    }

    private RelationResponse toRelationResponse(MemoryRelationRef relationRef) {
        return new RelationResponse(
                relationRef.id,
                relationRef.sourceMemoryId,
                relationRef.targetMemoryId,
                relationRef.relationType,
                relationRef.createdAt);
    }
}

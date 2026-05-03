package com.btl.administrador.api.service;

import com.btl.administrador.api.domain.CaseRecord;
import com.btl.administrador.api.domain.CaseStatus;
import com.btl.administrador.api.dto.CaseResponse;
import com.btl.administrador.api.dto.CloseCaseRequest;
import com.btl.administrador.api.dto.CreateCaseRequest;
import com.btl.administrador.api.exception.ApiException;
import com.btl.administrador.api.persistence.CaseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jboss.logging.Logger;

@ApplicationScoped
public class CaseService {

    private static final Logger LOG = Logger.getLogger(CaseService.class);
    private static final String CASE_CLOSED_ACTION = "CASE_CLOSED";

    @Inject
    CaseRepository caseRepository;

    @Inject
    AuditService auditService;

    @Inject
    StructuredExtractionService structuredExtractionService;

    private static final Set<String> VALID_PRIORITIES = Set.of("BAJA", "MEDIA", "ALTA", "CRITICA");

    @Transactional
    public CaseResponse create(CreateCaseRequest request) {
        // ISSUE #5: Programmatic validation as defense-in-depth for Bean Validation
        String normalizedPriority = request.priority().toUpperCase(Locale.ROOT);
        if (!VALID_PRIORITIES.contains(normalizedPriority)) {
            throw new ApiException(Response.Status.BAD_REQUEST.getStatusCode(), "INVALID_PRIORITY",
                    "priority must be one of: BAJA, MEDIA, ALTA, CRITICA");
        }

        CaseRecord caseRecord = new CaseRecord();
        caseRecord.id = "CASO-" + UUID.randomUUID().toString().substring(0, 8);
        caseRecord.origin = request.origin();
        caseRecord.title = request.title();
        caseRecord.description = request.description();
        caseRecord.priority = request.priority();
        caseRecord.domain = request.domain();
        caseRecord.criticality = request.criticality();
        caseRecord.tags = safeList(request.tags());
        caseRecord.participants = safeList(request.participants());
        caseRecord.status = CaseStatus.ABIERTO;
        caseRecord.createdAt = OffsetDateTime.now();
        caseRepository.save(caseRecord);

        // Trigger AI extraction to enrich case with tags and domain suggestions
        try {
            Map<String, String> extractionResult = structuredExtractionService.enrichMetadata(
                    "## Descripcion\n\n" + request.description(),
                    Map.of("fuente", "caso"));
            if (caseRecord.tags.isEmpty() && extractionResult.containsKey("extractedTags")) {
                String extractedTags = extractionResult.get("extractedTags");
                caseRecord.tags = List.of(extractedTags.split(","));
                caseRepository.save(caseRecord);
            }
        } catch (Exception e) {
            LOG.warnv("AI extraction for case {0} failed (non-blocking): {1}", caseRecord.id, e.getMessage());
        }

        auditService.record(caseRecord.id, "CASE", "CASE_CREATED", "Caso creado", null, null);
        return toResponse(caseRecord);
    }

    public CaseRecord requireById(String caseId) {
        return caseRepository.findById(caseId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND.getStatusCode(), "CASE_NOT_FOUND", "Case not found"));
    }

    public CaseResponse getById(String caseId) {
        return toResponse(requireById(caseId));
    }

    @Transactional
    public CaseResponse close(String caseId, CloseCaseRequest request) {
        CaseRecord caseRecord = requireById(caseId);
        if (caseRecord.status == CaseStatus.CERRADO) {
            throw new ApiException(Response.Status.CONFLICT.getStatusCode(), "INVALID_STATE", "Case is already closed");
        }

        caseRecord.status = CaseStatus.CERRADO;
        caseRepository.save(caseRecord);
        auditService.record(caseRecord.id, "CASE", CASE_CLOSED_ACTION,
                structuredDetail(request.resultadoOperativo(), request.memoryId(), request.observaciones()), null, null);
        return toResponse(caseRecord);
    }

    private CaseResponse toResponse(CaseRecord caseRecord) {
        var closureEvent = auditService.findLatestByAction(caseRecord.id, CASE_CLOSED_ACTION).orElse(null);
        String closureResult = null;
        String closureMemoryId = null;
        OffsetDateTime closedAt = null;
        if (closureEvent != null) {
            closureResult = extractDetailValue(closureEvent.detail(), "resultadoOperativo");
            closureMemoryId = extractDetailValue(closureEvent.detail(), "memoryId");
            closedAt = closureEvent.createdAt();
        }

        return new CaseResponse(
                caseRecord.id,
                caseRecord.origin,
                caseRecord.title,
                caseRecord.description,
                caseRecord.priority,
                caseRecord.domain,
                caseRecord.criticality,
                caseRecord.tags,
                caseRecord.participants,
                caseRecord.status,
                caseRecord.createdAt,
                closureResult,
                closureMemoryId,
                closedAt);
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values.stream().filter(value -> value != null && !value.isBlank()).toList();
    }

    private String structuredDetail(String result, String memoryId, String observations) {
        return "resultadoOperativo=" + escape(result)
                + ";memoryId=" + escape(memoryId)
                + ";observaciones=" + escape(observations);
    }

    private String extractDetailValue(String detail, String key) {
        if (detail == null || detail.isBlank()) {
            return null;
        }

        for (String token : detail.split(";")) {
            String[] parts = token.split("=", 2);
            if (parts.length == 2 && parts[0].equals(key)) {
                return unescape(parts[1]);
            }
        }
        return null;
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("%", "%25").replace(";", "%3B").replace("=", "%3D");
    }

    private String unescape(String value) {
        return value == null || value.isBlank()
                ? null
                : value.replace("%3B", ";").replace("%3D", "=").replace("%25", "%");
    }
}

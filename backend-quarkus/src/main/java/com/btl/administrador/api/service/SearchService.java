package com.btl.administrador.api.service;

import com.btl.administrador.api.domain.MemoryRecord;
import com.btl.administrador.api.domain.MemoryOrigin;
import com.btl.administrador.api.domain.MemoryState;
import com.btl.administrador.api.domain.ProcessingStatus;
import com.btl.administrador.api.domain.Criticality;
import com.btl.administrador.api.dto.SearchFiltersRequest;
import com.btl.administrador.api.dto.SearchMemoryRequest;
import com.btl.administrador.api.dto.SearchResultResponse;
import com.btl.administrador.api.exception.ApiException;
import com.btl.administrador.api.integration.qdrant.SearchIndexer;
import com.btl.administrador.api.persistence.MemoryRepository;
import com.btl.administrador.api.security.MemoryRoles;
import com.btl.administrador.api.service.model.SearchHit;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@ApplicationScoped
public class SearchService {

    @Inject
    SearchIndexer searchIndexer;

    @Inject
    MemoryRepository memoryRepository;

    @Inject
    SecurityIdentity securityIdentity;

    public List<SearchResultResponse> search(SearchMemoryRequest request) {
        if (request.topK() <= 0 || request.topK() > 50) {
            throw new ApiException(Response.Status.BAD_REQUEST.getStatusCode(), "INVALID_TOPK", "topK must be between 1 and 50");
        }

        validateFilters(request.filtros());

        // ISSUE #9: api-consumer only sees APROBADA memories
        Predicate<MemoryRecord> roleFilter = buildRoleFilter();

        Map<String, MemoryRecord> available = memoryRepository.findAll().stream()
                .filter(memory -> memory.processingStatus == ProcessingStatus.AVAILABLE)
                .filter(defaultVisibilityFilter(request.filtros()))
                .filter(roleFilter)
                .collect(Collectors.toMap(memory -> memory.id, memory -> memory));

        List<SearchHit> hits = searchIndexer.search(request.consulta(), request.topK(), request.filtros());
        List<SearchResultResponse> results = new ArrayList<>();
        for (SearchHit hit : hits) {
            MemoryRecord memory = available.get(hit.memoryId());
            if (memory != null && matchesFilters(memory, request.filtros())) {
                results.add(new SearchResultResponse(
                        memory.id,
                        memory.title,
                        summarize(memory.currentMarkdown),
                        hit.score(),
                        memory.state,
                        memory.origin,
                        memory.criticality,
                        memory.domains,
                        memory.tags,
                        memory.commitSha));
            }
        }

        return results.stream()
                .sorted(Comparator.comparingDouble(SearchResultResponse::score).reversed())
                .toList();
    }

    private void validateFilters(SearchFiltersRequest filters) {
        if (filters == null) {
            return;
        }

        validateEnumValues(filters.states(), MemoryState.class, "states");
        validateEnumValues(filters.origins(), MemoryOrigin.class, "origins");
        validateEnumValues(filters.criticalities(), Criticality.class, "criticalities");
    }

    private <E extends Enum<E>> void validateEnumValues(List<String> values, Class<E> enumClass, String filterName) {
        if (values == null) {
            return;
        }

        for (String value : values) {
            try {
                Enum.valueOf(enumClass, value.toUpperCase());
            } catch (IllegalArgumentException exception) {
                throw new ApiException(Response.Status.BAD_REQUEST.getStatusCode(), "INVALID_FILTER", "Unsupported " + filterName + " filter");
            }
        }
    }

    /**
     * ISSUE #9: If the current user only has the api-consumer role (and no operator/reviewer/admin/auditor),
     * restrict results to APROBADA memories only. Users with elevated roles see all non-excluded states.
     */
    private Predicate<MemoryRecord> buildRoleFilter() {
        if (securityIdentity == null || securityIdentity.isAnonymous()) {
            // No authenticated user: strictest filter (APROBADA only)
            return memory -> memory.state == MemoryState.APROBADA;
        }

        boolean isApiConsumerOnly = securityIdentity.hasRole(MemoryRoles.API_CONSUMER)
                && !securityIdentity.hasRole(MemoryRoles.MEMORY_OPERATOR)
                && !securityIdentity.hasRole(MemoryRoles.MEMORY_REVIEWER)
                && !securityIdentity.hasRole(MemoryRoles.MEMORY_ADMIN)
                && !securityIdentity.hasRole(MemoryRoles.MEMORY_AUDITOR);

        if (isApiConsumerOnly) {
            return memory -> memory.state == MemoryState.APROBADA;
        }

        // Operator, reviewer, admin, auditor: see all states (filtered by defaultVisibilityFilter)
        return memory -> true;
    }

    private Predicate<MemoryRecord> defaultVisibilityFilter(SearchFiltersRequest filters) {
        boolean includeArchived = filters != null && Boolean.TRUE.equals(filters.includeArchived());
        if (includeArchived) {
            return memory -> memory.state != MemoryState.ELIMINADA && memory.state != MemoryState.DUPLICADA;
        }
        return memory -> memory.state != MemoryState.ARCHIVADA
                && memory.state != MemoryState.ELIMINADA
                && memory.state != MemoryState.DUPLICADA
                && memory.state != MemoryState.RECHAZADA;
    }

    private boolean matchesFilters(MemoryRecord memory, SearchFiltersRequest filters) {
        if (filters == null) {
            return true;
        }

        return matchesAny(filters.domains(), memory.domains)
                && matchesValue(filters.states(), memory.state.name())
                && matchesValue(filters.origins(), memory.origin.name())
                && matchesValue(filters.types(), memory.type)
                && matchesAny(filters.tags(), memory.tags)
                && matchesValue(filters.criticalities(), memory.criticality.name());
    }

    private boolean matchesAny(List<String> filters, List<String> values) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }
        return values.stream().anyMatch(value -> filters.stream().anyMatch(filter -> value.equalsIgnoreCase(filter)));
    }

    private boolean matchesValue(List<String> filters, String value) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }
        return filters.stream().anyMatch(filter -> value.equalsIgnoreCase(filter));
    }

    private String summarize(String markdown) {
        String sanitized = markdown == null ? "" : markdown.replace("---", " ").replaceAll("\\s+", " ").trim();
        if (sanitized.length() <= 120) {
            return sanitized;
        }
        return sanitized.substring(0, 117) + "...";
    }
}

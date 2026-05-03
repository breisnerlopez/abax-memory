package com.btl.administrador.api.service;

import com.btl.administrador.api.domain.Criticality;
import com.btl.administrador.api.domain.MemoryOrigin;
import com.btl.administrador.api.domain.MemoryRecord;
import com.btl.administrador.api.domain.MemoryState;
import com.btl.administrador.api.domain.ProcessingStatus;
import com.btl.administrador.api.dto.SearchFiltersRequest;
import com.btl.administrador.api.dto.SearchMemoryRequest;
import com.btl.administrador.api.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchServiceTest {

    @Test
    void search_invalidTopK_throwsValidationError() {
        ServiceTestSupport support = new ServiceTestSupport();

        assertThatThrownBy(() -> support.searchService.search(new SearchMemoryRequest("consulta", 0, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> {
                    ApiException exception = (ApiException) error;
                    assertThat(exception.getStatus()).isEqualTo(400);
                    assertThat(exception.getCode()).isEqualTo("INVALID_TOPK");
                });
    }

    @Test
    void search_invalidStructuredStateFilter_throwsValidationError() {
        ServiceTestSupport support = new ServiceTestSupport();

        assertThatThrownBy(() -> support.searchService.search(new SearchMemoryRequest(
                "consulta",
                10,
                new SearchFiltersRequest(null, List.of("desconocido"), null, null, null, null, false))))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> {
                    ApiException exception = (ApiException) error;
                    assertThat(exception.getStatus()).isEqualTo(400);
                    assertThat(exception.getCode()).isEqualTo("INVALID_FILTER");
                });
    }

    @Test
    void search_defaultVisibility_excludesArchivedAndNonSearchableStates() {
        ServiceTestSupport support = new ServiceTestSupport();
        saveIndexedMemory(support, "MEM-1", "Manual onboarding", MemoryState.APROBADA, ProcessingStatus.AVAILABLE,
                MemoryOrigin.MANUAL, Criticality.BAJA, List.of("RRHH"), List.of("guia"), "manual onboarding para ingresos");
        saveIndexedMemory(support, "MEM-2", "Pendiente index final", MemoryState.APROBADA, ProcessingStatus.PENDING_INDEX,
                MemoryOrigin.CASO, Criticality.MEDIA, List.of("RRHH"), List.of("pendiente"), "manual onboarding con datos pendientes");
        saveIndexedMemory(support, "MEM-3", "Archivada onboarding", MemoryState.ARCHIVADA, ProcessingStatus.AVAILABLE,
                MemoryOrigin.MANUAL, Criticality.BAJA, List.of("RRHH"), List.of("archivo"), "manual onboarding archivado");
        saveIndexedMemory(support, "MEM-4", "Rechazada onboarding", MemoryState.RECHAZADA, ProcessingStatus.AVAILABLE,
                MemoryOrigin.MANUAL, Criticality.BAJA, List.of("RRHH"), List.of("rechazo"), "manual onboarding rechazado");

        var results = support.searchService.search(new SearchMemoryRequest("manual onboarding ingresos", 10, null));

        assertThat(results).extracting(result -> result.memoryId())
                .containsExactly("MEM-1");
    }

    @Test
    void search_withStructuredFilters_returnsOnlyMatchingMemories() {
        ServiceTestSupport support = new ServiceTestSupport();
        saveIndexedMemory(support, "MEM-1", "Runbook RRHH", MemoryState.APROBADA, ProcessingStatus.AVAILABLE,
                MemoryOrigin.MANUAL, Criticality.BAJA, List.of("RRHH"), List.of("guia"), "runbook onboarding rrhh");
        saveIndexedMemory(support, "MEM-2", "Runbook Seguridad", MemoryState.APROBADA, ProcessingStatus.AVAILABLE,
                MemoryOrigin.CASO, Criticality.ALTA, List.of("SEGURIDAD"), List.of("incidente"), "runbook onboarding seguridad");

        var filters = new SearchFiltersRequest(
                List.of("seguridad"),
                List.of("aprobada"),
                List.of("caso"),
                List.of("runbook"),
                List.of("incidente"),
                List.of("alta"),
                false);

        var results = support.searchService.search(new SearchMemoryRequest("runbook onboarding", 10, filters));

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.memoryId()).isEqualTo("MEM-2");
            assertThat(result.title()).isEqualTo("Runbook Seguridad");
        });
    }

    @Test
    void search_semanticEquivalentQuery_returnsRelatedMemoryWithoutExactMatch() {
        ServiceTestSupport support = new ServiceTestSupport();
        saveIndexedMemory(support, "MEM-1", "Reset de credenciales", MemoryState.APROBADA, ProcessingStatus.AVAILABLE,
                MemoryOrigin.MANUAL, Criticality.BAJA, List.of("SOPORTE"), List.of("password"), "restablecer contrasena del usuario afectado");

        var results = support.searchService.search(new SearchMemoryRequest("reiniciar clave de acceso", 10, null));

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.memoryId()).isEqualTo("MEM-1");
            assertThat(result.score()).isGreaterThan(0.0d);
        });
    }

    @Test
    void search_multipleRelevantMatches_returnsOrderedByDescendingScore() {
        ServiceTestSupport support = new ServiceTestSupport();
        saveIndexedMemory(support, "MEM-1", "Reset de credenciales completo", MemoryState.APROBADA, ProcessingStatus.AVAILABLE,
                MemoryOrigin.MANUAL, Criticality.BAJA, List.of("SOPORTE"), List.of("password"),
                "restablecer contrasena del usuario y recuperar acceso a la cuenta");
        saveIndexedMemory(support, "MEM-2", "Guia de operador", MemoryState.APROBADA, ProcessingStatus.AVAILABLE,
                MemoryOrigin.MANUAL, Criticality.BAJA, List.of("SOPORTE"), List.of("usuario"),
                "orientacion para persona asignada");

        var results = support.searchService.search(new SearchMemoryRequest("reiniciar clave de acceso usuario", 10, null));

        assertThat(results).hasSize(2);
        assertThat(results).extracting(result -> result.memoryId())
                .containsExactly("MEM-1", "MEM-2");
        assertThat(results.get(0).score()).isGreaterThan(results.get(1).score());
    }

    @Test
    void search_includeArchived_allowsArchivedButStillExcludesDeleted() {
        ServiceTestSupport support = new ServiceTestSupport();
        saveIndexedMemory(support, "MEM-1", "Archivada util", MemoryState.ARCHIVADA, ProcessingStatus.AVAILABLE,
                MemoryOrigin.MANUAL, Criticality.BAJA, List.of("LEGAL"), List.of("historia"), "memoria util archivada");
        saveIndexedMemory(support, "MEM-2", "Eliminada util", MemoryState.ELIMINADA, ProcessingStatus.AVAILABLE,
                MemoryOrigin.MANUAL, Criticality.BAJA, List.of("LEGAL"), List.of("historia"), "memoria util eliminada");

        var results = support.searchService.search(new SearchMemoryRequest(
                "memoria util",
                10,
                new SearchFiltersRequest(null, null, null, null, null, null, true)));

        assertThat(results).extracting(result -> result.memoryId()).containsExactly("MEM-1");
    }

    private void saveIndexedMemory(ServiceTestSupport support,
                                   String id,
                                   String title,
                                   MemoryState state,
                                   ProcessingStatus processingStatus,
                                   MemoryOrigin origin,
                                   Criticality criticality,
                                   List<String> domains,
                                   List<String> tags,
                                   String markdown) {
        MemoryRecord record = new MemoryRecord();
        record.id = id;
        record.title = title;
        record.type = "RUNBOOK";
        record.state = state;
        record.processingStatus = processingStatus;
        record.origin = origin;
        record.criticality = criticality;
        record.domains = domains;
        record.tags = tags;
        record.metadata = Map.of("fuente", "manual");
        record.currentMarkdown = markdown;
        record.commitSha = "commit-" + id;
        record.createdAt = OffsetDateTime.now();
        record.updatedAt = record.createdAt;
        support.memoryRepository.save(record);
        support.searchIndexer.index(id, title, markdown);
    }
}

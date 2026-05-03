package com.btl.administrador.api.service;

import com.btl.administrador.api.domain.Criticality;
import com.btl.administrador.api.domain.MemoryState;
import com.btl.administrador.api.domain.ProcessingJobType;
import com.btl.administrador.api.domain.ProcessingStatus;
import com.btl.administrador.api.domain.ReviewDecision;
import com.btl.administrador.api.dto.ApproveMemoryRequest;
import com.btl.administrador.api.dto.CreateCaseRequest;
import com.btl.administrador.api.dto.CreateMemoryFromCaseRequest;
import com.btl.administrador.api.dto.CreateMemoryRequest;
import com.btl.administrador.api.dto.MemoryResponse;
import com.btl.administrador.api.dto.ReviewMemoryRequest;
import com.btl.administrador.api.dto.UpdateMemoryRequest;
import com.btl.administrador.api.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemoryServiceTest {

    @Test
    void createManual_lowCriticality_persistsApprovedMemoryAndCreatesIndexJob() {
        ServiceTestSupport support = new ServiceTestSupport();

        MemoryResponse response = support.memoryService.createManual(new CreateMemoryRequest(
                "Guia de onboarding",
                "RUNBOOK",
                Criticality.BAJA,
                List.of("RRHH", "", "OPERACIONES"),
                Arrays.asList("alta", "", null),
                "## Pasos\n- Revisar checklist\n## Evidencias\n- Ticket aprobado\n## Resultado\n- Onboarding completado",
                Map.of("fuente", "manual", "autor", "qa\"team"),
                frontmatter("Guia de onboarding", "runbook", "manual", "baja", List.of("RRHH", "OPERACIONES"), "manual")));

        assertThat(response.id()).startsWith("MEM-");
        assertThat(response.state()).isEqualTo(MemoryState.APROBADA);
        assertThat(response.processingStatus()).isEqualTo(ProcessingStatus.PENDING_INDEX);
        assertThat(response.commitSha()).startsWith("commit-");
        assertThat(response.pullRequestRef()).isNull();
        assertThat(response.domains()).containsExactly("RRHH", "OPERACIONES");
        assertThat(response.tags()).containsExactly("alta");
        assertThat(response.markdown()).contains("title: \"Guia de onboarding\"");
        assertThat(response.markdown()).contains("autor: \"qa'team\"");
        assertThat(response.metadata()).containsEntry("extractionStatus", "PARTIAL");
        assertThat(response.metadata()).containsEntry("extractedSteps", "Revisar checklist");
        assertThat(support.processingJobRepository.findByStatus(com.btl.administrador.api.domain.ProcessingJobStatus.PENDING))
                .singleElement()
                .satisfies(job -> {
                    assertThat(job.memoryId).isEqualTo(response.id());
                    assertThat(job.versionId).isEqualTo(response.versionId());
                    assertThat(job.jobType).isEqualTo(ProcessingJobType.INDEX_MEMORY);
                });
        assertThat(support.auditService.findByEntityId(response.id()))
                .singleElement()
                .extracting(event -> event.action())
                .isEqualTo("MEMORY_CREATED");
    }

    @Test
    void createManual_highCriticality_submitsForReviewAndCreatesIndexJob() {
        ServiceTestSupport support = new ServiceTestSupport();

        MemoryResponse response = support.memoryService.createManual(new CreateMemoryRequest(
                "Hallazgo critico",
                "INCIDENTE",
                Criticality.ALTA,
                List.of("SEGURIDAD"),
                List.of("pr"),
                "# Debe aprobarse",
                Map.of("fuente", "manual"),
                frontmatter("Hallazgo critico", "incidente", "manual", "alta", List.of("SEGURIDAD"), "manual")));

        assertThat(response.state()).isEqualTo(MemoryState.EN_REVISION);
        assertThat(response.processingStatus()).isEqualTo(ProcessingStatus.PENDING_GIT);
        assertThat(response.pullRequestRef()).startsWith("PR-");
        // ISSUE #3 fix: EN_REVISION memories now get indexed for semantic search
        assertThat(support.processingJobRepository.findByStatus(com.btl.administrador.api.domain.ProcessingJobStatus.PENDING))
                .hasSize(1)
                .singleElement()
                .satisfies(job -> {
                    assertThat(job.memoryId).isEqualTo(response.id());
                    assertThat(job.jobType).isEqualTo(ProcessingJobType.INDEX_MEMORY);
                });
        assertThat(support.auditService.findByEntityId(response.id()))
                .extracting(event -> event.action())
                .anySatisfy(action -> assertThat(action).isEqualTo("MEMORY_SUBMITTED_FOR_REVIEW"));
    }

    @Test
    void createManual_missingMetadata_throwsValidationError() {
        ServiceTestSupport support = new ServiceTestSupport();

        assertThatThrownBy(() -> support.memoryService.createManual(new CreateMemoryRequest(
                "Titulo",
                "RUNBOOK",
                Criticality.MEDIA,
                List.of("LEGAL"),
                List.of(),
                "texto",
                Map.of("autor", "ana"),
                frontmatter("Titulo", "runbook", "manual", "media", List.of("LEGAL"), "manual"))))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> {
                    ApiException exception = (ApiException) error;
                    assertThat(exception.getStatus()).isEqualTo(400);
                    assertThat(exception.getCode()).isEqualTo("INVALID_METADATA");
                });
    }

    @Test
    void createManual_invalidFrontmatter_throwsValidationError() {
        ServiceTestSupport support = new ServiceTestSupport();

        assertThatThrownBy(() -> support.memoryService.createManual(new CreateMemoryRequest(
                "Titulo",
                "RUNBOOK",
                Criticality.MEDIA,
                List.of("LEGAL"),
                List.of(),
                "texto",
                Map.of("fuente", "manual"),
                frontmatter("Otro titulo", "runbook", "manual", "media", List.of("LEGAL"), "manual"))))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getCode()).isEqualTo("INVALID_FRONTMATTER"));
    }

    @Test
    void createManual_invalidType_throwsValidationError() {
        ServiceTestSupport support = new ServiceTestSupport();

        assertThatThrownBy(() -> support.memoryService.createManual(new CreateMemoryRequest(
                "Titulo",
                "LIBRE",
                Criticality.MEDIA,
                List.of("LEGAL"),
                List.of(),
                "texto",
                Map.of("fuente", "manual"),
                frontmatter("Titulo", "libre", "manual", "media", List.of("LEGAL"), "manual"))))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getCode()).isEqualTo("INVALID_MEMORY_TYPE"));
    }

    @Test
    void createFromCase_existingCase_mergesMetadataAndSourceReference() {
        ServiceTestSupport support = new ServiceTestSupport();
        var createdCase = support.caseService.create(new CreateCaseRequest(
                "jira",
                "Caso de fraude",
                "Descripcion del caso",
                "alta",
                "RIESGO",
                Criticality.MEDIA,
                List.of("fraude"),
                List.of("analista")));

        MemoryResponse response = support.memoryService.createFromCase(new CreateMemoryFromCaseRequest(
                createdCase.id(),
                "Memoria derivada",
                "CASO",
                Criticality.BAJA,
                List.of("RIESGO"),
                List.of("evidencia"),
                Map.of("fuente", "externa", "origenCaso", "jira"),
                frontmatter("Memoria derivada", "caso", "caso", "baja", List.of("RIESGO"), "externa")));

        assertThat(response.sourceCaseId()).isEqualTo(createdCase.id());
        assertThat(response.metadata()).containsEntry("caseTitle", "Caso de fraude");
        assertThat(response.metadata()).containsEntry("fuente", "externa");
        assertThat(response.markdown()).contains("sourceCaseId: " + createdCase.id());
        assertThat(response.markdown()).contains("# Contexto del caso");
        assertThat(response.markdown()).contains("Caso de fraude");
    }

    @Test
    void approve_reviewMemory_transitionsToApprovedAndCreatesIndexJob() {
        ServiceTestSupport support = new ServiceTestSupport();
        MemoryResponse created = support.memoryService.createManual(new CreateMemoryRequest(
                "Memoria a aprobar",
                "POLITICA",
                Criticality.CRITICA,
                List.of("SEGURIDAD"),
                List.of("revision"),
                "# Contenido",
                Map.of("fuente", "manual"),
                frontmatter("Memoria a aprobar", "politica", "manual", "critica", List.of("SEGURIDAD"), "manual")));

        MemoryResponse approved = support.memoryService.approve(created.id(), new ApproveMemoryRequest("Aprobada por revisor"));

        assertThat(approved.state()).isEqualTo(MemoryState.APROBADA);
        assertThat(approved.processingStatus()).isEqualTo(ProcessingStatus.PENDING_INDEX);
        assertThat(approved.commitSha()).startsWith("commit-");
        assertThat(support.processingJobRepository.findByStatus(com.btl.administrador.api.domain.ProcessingJobStatus.PENDING)).hasSize(1);
        assertThat(support.auditService.findByEntityId(created.id()))
                .extracting(event -> event.action())
                .containsExactly("MEMORY_SUBMITTED_FOR_REVIEW", "MEMORY_APPROVED");
    }

    @Test
    void review_reviewMemory_transitionsToObservedAndKeepsUnavailable() {
        ServiceTestSupport support = new ServiceTestSupport();
        MemoryResponse created = support.memoryService.createManual(new CreateMemoryRequest(
                "Memoria observada",
                "POLITICA",
                Criticality.CRITICA,
                List.of("SEGURIDAD"),
                List.of("revision"),
                "# Contenido",
                Map.of("fuente", "manual"),
                frontmatter("Memoria observada", "politica", "manual", "critica", List.of("SEGURIDAD"), "manual")));

        MemoryResponse reviewed = support.memoryService.review(created.id(), new ReviewMemoryRequest(ReviewDecision.OBSERVADA, "Faltan evidencias"));

        assertThat(reviewed.state()).isEqualTo(MemoryState.OBSERVADA);
        assertThat(support.auditService.findByEntityId(created.id()))
                .extracting(event -> event.action())
                .containsExactly("MEMORY_SUBMITTED_FOR_REVIEW", "MEMORY_REVIEW_DECISION");
    }

    @Test
    void update_existingMemory_createsNewVersionAndAuditTrace() {
        ServiceTestSupport support = new ServiceTestSupport();
        MemoryResponse created = support.memoryService.createManual(new CreateMemoryRequest(
                "Runbook base",
                "RUNBOOK",
                Criticality.BAJA,
                List.of("OPS"),
                List.of("base"),
                "## Pasos\n- Paso inicial",
                Map.of("fuente", "manual"),
                frontmatter("Runbook base", "runbook", "manual", "baja", List.of("OPS"), "manual")));

        MemoryResponse updated = support.memoryService.update(created.id(), new UpdateMemoryRequest(
                "Runbook actualizado",
                "procedimiento",
                List.of("OPS"),
                List.of("base", "v2"),
                "## Pasos\n- Paso inicial\n## Decisiones\n- Aprobar cambio\n## Evidencias\n- Commit validado\n## Resultados\n- Flujo actualizado",
                Map.of("fuente", "manual", "editor", "usuario-b"),
                frontmatter("Runbook actualizado", "procedimiento", "manual", "baja", List.of("OPS"), "manual")));

        assertThat(updated.title()).isEqualTo("Runbook actualizado");
        assertThat(updated.versionId()).isNotEqualTo(created.versionId());
        assertThat(updated.metadata()).containsEntry("extractionStatus", "COMPLETE");
        assertThat(support.auditService.findByEntityId(created.id()))
                .extracting(event -> event.action())
                .contains("MEMORY_UPDATED");
    }

    @Test
    void traceability_updateWithDifferentActors_exposesCreatorAndLastModifier() {
        ServiceTestSupport support = new ServiceTestSupport();
        support.useActor("creator-user", Set.of("memory-operator"));

        MemoryResponse created = support.memoryService.createManual(new CreateMemoryRequest(
                "Runbook base",
                "RUNBOOK",
                Criticality.BAJA,
                List.of("OPS"),
                List.of("base"),
                "## Pasos\n- Paso inicial",
                Map.of("fuente", "manual"),
                frontmatter("Runbook base", "runbook", "manual", "baja", List.of("OPS"), "manual")));

        support.useActor("editor-user", Set.of("memory-operator"));

        support.memoryService.update(created.id(), new UpdateMemoryRequest(
                "Runbook actualizado",
                "procedimiento",
                List.of("OPS"),
                List.of("base", "v2"),
                "## Pasos\n- Paso inicial\n## Decisiones\n- Aprobar cambio\n## Evidencias\n- Commit validado\n## Resultados\n- Flujo actualizado",
                Map.of("fuente", "manual", "editor", "usuario-b"),
                frontmatter("Runbook actualizado", "procedimiento", "manual", "baja", List.of("OPS"), "manual")));

        var traceability = support.memoryService.traceability(created.id());

        assertThat(traceability.createdBy()).isEqualTo("creator-user");
        assertThat(traceability.lastModifiedBy()).isEqualTo("editor-user");
        assertThat(traceability.events())
                .extracting(event -> event.actor())
                .containsExactly("creator-user", "editor-user");
    }

    @Test
    void approve_nonReviewMemory_throwsConflict() {
        ServiceTestSupport support = new ServiceTestSupport();
        MemoryResponse created = support.memoryService.createManual(new CreateMemoryRequest(
                "Memoria ya aprobada",
                "RUNBOOK",
                Criticality.BAJA,
                List.of("OPS"),
                List.of(),
                "# Contenido",
                Map.of("fuente", "manual"),
                frontmatter("Memoria ya aprobada", "runbook", "manual", "baja", List.of("OPS"), "manual")));

        assertThatThrownBy(() -> support.memoryService.approve(created.id(), new ApproveMemoryRequest("No aplica")))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> {
                    ApiException exception = (ApiException) error;
                    assertThat(exception.getStatus()).isEqualTo(409);
                    assertThat(exception.getCode()).isEqualTo("INVALID_STATE");
                });
    }

    private Map<String, Object> frontmatter(String title, String type, String origin, String criticality, List<String> domains, String fuente) {
        return Map.of(
                "title", title,
                "type", type,
                "origin", origin,
                "criticality", criticality,
                "domains", domains,
                "metadata", Map.of("fuente", fuente));
    }
}

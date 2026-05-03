package com.btl.administrador.api.service;

import com.btl.administrador.api.domain.Criticality;
import com.btl.administrador.api.domain.ProcessingJobType;
import com.btl.administrador.api.domain.MemoryState;
import com.btl.administrador.api.domain.ProcessingJobStatus;
import com.btl.administrador.api.domain.ProcessingStatus;
import com.btl.administrador.api.dto.CreateMemoryRequest;
import com.btl.administrador.api.dto.SearchFiltersRequest;
import com.btl.administrador.api.exception.ApiException;
import com.btl.administrador.api.integration.qdrant.SearchIndexer;
import com.btl.administrador.api.service.model.SearchHit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessingWorkerServiceTest {

    @Test
    void processPendingJobs_successfulIndexing_marksMemoryAvailableAndCompletesJob() {
        ServiceTestSupport support = new ServiceTestSupport();
        var created = support.memoryService.createManual(new CreateMemoryRequest(
                "Memoria indexable",
                "RUNBOOK",
                Criticality.BAJA,
                List.of("OPS"),
                List.of("job"),
                "# Cuerpo indexable",
                Map.of("fuente", "manual"),
                frontmatter("Memoria indexable", "runbook", List.of("OPS"))));

        support.processingWorkerService.processPendingJobs();

        var stored = support.memoryService.getById(created.id());
        assertThat(stored.state()).isEqualTo(MemoryState.APROBADA);
        assertThat(stored.processingStatus()).isEqualTo(ProcessingStatus.AVAILABLE);
        assertThat(support.processingJobRepository.findByStatus(ProcessingJobStatus.COMPLETED)).hasSize(1);
        assertThat(support.auditService.findByEntityId(created.id()))
                .extracting(event -> event.action())
                .containsExactly("MEMORY_CREATED", "MEMORY_INDEXED");
    }

    @Test
    void processPendingJobs_indexerFailure_marksMemoryAndJobAsFailed() {
        ServiceTestSupport support = new ServiceTestSupport();
        var created = support.memoryService.createManual(new CreateMemoryRequest(
                "Memoria fallida",
                "RUNBOOK",
                Criticality.BAJA,
                List.of("OPS"),
                List.of("fallo"),
                "# Cuerpo",
                Map.of("fuente", "manual"),
                frontmatter("Memoria fallida", "runbook", List.of("OPS"))));
        support.useSearchIndexer(new FailingSearchIndexer());

        support.processingWorkerService.processPendingJobs();

        assertThat(support.processingJobRepository.findByStatus(ProcessingJobStatus.PENDING))
                .singleElement()
                .satisfies(job -> {
                    assertThat(job.retryCount).isEqualTo(1);
                    assertThat(job.lastError).isEqualTo("forced-index-error");
                    assertThat(job.nextAttemptAt).isNotNull();
                });

        support.processingWorkerService.processPendingJobs();
        support.processingWorkerService.processPendingJobs();

        var stored = support.memoryService.getById(created.id());
        assertThat(stored.processingStatus()).isEqualTo(ProcessingStatus.INDEX_FAILED);
        assertThat(support.processingJobRepository.findByStatus(ProcessingJobStatus.FAILED))
                .singleElement()
                .satisfies(job -> {
                    assertThat(job.retryCount).isEqualTo(3);
                    assertThat(job.lastError).isEqualTo("forced-index-error");
                    assertThat(job.nextAttemptAt).isNull();
                });
        assertThat(support.auditService.findByEntityId(created.id()))
                .extracting(event -> event.action())
                .containsExactly("MEMORY_CREATED");
    }

    @Test
    void processPendingJobs_nonIndexJob_leavesJobPendingWithoutSideEffects() {
        ServiceTestSupport support = new ServiceTestSupport();
        var created = support.memoryService.createManual(new CreateMemoryRequest(
                "Memoria reconciliable",
                "RUNBOOK",
                Criticality.BAJA,
                List.of("OPS"),
                List.of("reconcile"),
                "# Cuerpo",
                Map.of("fuente", "manual"),
                frontmatter("Memoria reconciliable", "runbook", List.of("OPS"))));
        support.processingJobRepository.clear();
        support.processingJobService.createIfAbsent(created.id(), created.versionId(), ProcessingJobType.RECONCILE_MEMORY);

        support.processingWorkerService.processPendingJobs();

        assertThat(support.processingJobRepository.findByStatus(ProcessingJobStatus.PENDING)).singleElement()
                .satisfies(job -> assertThat(job.jobType).isEqualTo(ProcessingJobType.RECONCILE_MEMORY));
        assertThat(support.memoryService.getById(created.id()).processingStatus()).isEqualTo(ProcessingStatus.PENDING_INDEX);
    }

    @Test
    void processPendingJobs_missingVersion_marksMemoryAndJobAsFailed() {
        ServiceTestSupport support = new ServiceTestSupport();
        var created = support.memoryService.createManual(new CreateMemoryRequest(
                "Memoria sin version",
                "RUNBOOK",
                Criticality.BAJA,
                List.of("OPS"),
                List.of("missing-version"),
                "# Cuerpo",
                Map.of("fuente", "manual"),
                frontmatter("Memoria sin version", "runbook", List.of("OPS"))));
        var pendingJob = support.processingJobRepository.findByStatus(ProcessingJobStatus.PENDING).get(0);
        pendingJob.versionId = "version-inexistente";
        support.processingJobRepository.save(pendingJob);

        support.processingWorkerService.processPendingJobs();

        assertThat(support.memoryService.getById(created.id()).processingStatus()).isEqualTo(ProcessingStatus.INDEX_FAILED);
        assertThat(support.processingJobRepository.findByStatus(ProcessingJobStatus.PENDING)).singleElement()
                .satisfies(job -> assertThat(job.retryCount).isEqualTo(1));
    }

    private static class FailingSearchIndexer implements SearchIndexer {

        @Override
        public void index(String memoryId, String title, String markdown) {
            throw new ApiException(500, "INDEX_ERROR", "forced-index-error");
        }

        @Override
        public List<SearchHit> search(String query, int topK, SearchFiltersRequest filters) {
            return List.of();
        }

        @Override
        public void clear() {
        }
    }

    private static Map<String, Object> frontmatter(String title, String type, List<String> domains) {
        return Map.of(
                "title", title,
                "type", type,
                "origin", "manual",
                "criticality", "baja",
                "domains", domains,
                "metadata", Map.of("fuente", "manual"));
    }
}

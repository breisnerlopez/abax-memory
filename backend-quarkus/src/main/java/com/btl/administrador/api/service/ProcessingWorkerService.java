package com.btl.administrador.api.service;

import com.btl.administrador.api.domain.MemoryRecord;
import com.btl.administrador.api.domain.ProcessingJob;
import com.btl.administrador.api.domain.ProcessingJobType;
import com.btl.administrador.api.domain.ProcessingStatus;
import com.btl.administrador.api.integration.qdrant.SearchIndexer;
import com.btl.administrador.api.persistence.MemoryRepository;
import com.btl.administrador.api.persistence.MemoryVersionRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ProcessingWorkerService {

    private static final Logger LOG = Logger.getLogger(ProcessingWorkerService.class);

    private final String workerId = "memory-worker-" + UUID.randomUUID();

    @Inject
    ProcessingJobService processingJobService;

    @Inject
    MemoryRepository memoryRepository;

    @Inject
    MemoryVersionRepository memoryVersionRepository;

    @Inject
    SearchIndexer searchIndexer;

    @Inject
    AuditService auditService;

    @ConfigProperty(name = "abax.processing.auto-run", defaultValue = "true")
    boolean autoRun;

    @ConfigProperty(name = "abax.processing.batch-size", defaultValue = "10")
    int batchSize = 10;

    @Scheduled(every = "10s")
    void scheduledProcessing() {
        if (autoRun) {
            processPendingJobs();
        }
    }

    public void processPendingJobs() {
        List<ProcessingJob> claimedJobs = processingJobService.claimPendingJobs(workerId, batchSize);
        LOG.infov("Claimed {0} processing jobs", claimedJobs.size());
        for (ProcessingJob job : claimedJobs) {
            if (job.jobType != ProcessingJobType.INDEX_MEMORY) {
                processingJobService.markFailed(job, "Unsupported job type: " + job.jobType);
                continue;
            }

            try {
                MemoryRecord memory = memoryRepository.findById(job.memoryId).orElseThrow();
                String markdown = memoryVersionRepository.findById(job.versionId).orElseThrow().markdownContent;
                memory.processingStatus = ProcessingStatus.INDEXING;
                memoryRepository.save(memory);
                searchIndexer.index(memory.id, memory.title, markdown);
                memory.processingStatus = ProcessingStatus.AVAILABLE;
                memoryRepository.save(memory);
                processingJobService.markCompleted(job);
                auditService.record(memory.id, "MEMORY", "MEMORY_INDEXED", "Memoria indexada para busqueda", memory.commitSha, memory.pullRequestRef);
            } catch (Exception exception) {
                LOG.errorv(exception, "Failed processing job {0}", job.id);
                memoryRepository.findById(job.memoryId).ifPresent(memory -> {
                    memory.processingStatus = ProcessingStatus.INDEX_FAILED;
                    memoryRepository.save(memory);
                });
                processingJobService.markFailed(job, exception.getMessage());
            }
        }
    }
}

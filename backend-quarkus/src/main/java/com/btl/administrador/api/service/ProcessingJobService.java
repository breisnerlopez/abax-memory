package com.btl.administrador.api.service;

import com.btl.administrador.api.domain.ProcessingJob;
import com.btl.administrador.api.domain.ProcessingJobStatus;
import com.btl.administrador.api.domain.ProcessingJobType;
import com.btl.administrador.api.persistence.ProcessingJobRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ProcessingJobService {

    @Inject
    ProcessingJobRepository processingJobRepository;

    @ConfigProperty(name = "abax.processing.retry-delay", defaultValue = "PT30S")
    Duration retryDelay = Duration.ofSeconds(30);

    @ConfigProperty(name = "abax.processing.max-retries", defaultValue = "3")
    int maxRetries = 3;

    public ProcessingJob createIfAbsent(String memoryId, String versionId, ProcessingJobType jobType) {
        return processingJobRepository.findExisting(memoryId, versionId, jobType)
                .orElseGet(() -> persistNewJob(memoryId, versionId, jobType));
    }

    public List<ProcessingJob> pendingJobs() {
        return processingJobRepository.findByStatus(ProcessingJobStatus.PENDING);
    }

    public List<ProcessingJob> claimPendingJobs(String workerId, int batchSize) {
        return processingJobRepository.claimPendingJobs(workerId, batchSize, OffsetDateTime.now());
    }

    public void markCompleted(ProcessingJob job) {
        job.status = ProcessingJobStatus.COMPLETED;
        job.lockedBy = null;
        job.lockedAt = null;
        job.nextRetryAt = null;
        job.lastError = null;
        job.updatedAt = OffsetDateTime.now();
        processingJobRepository.save(job);
    }

    public void markFailed(ProcessingJob job, String error) {
        job.retryCount = job.retryCount + 1;
        job.lastError = error;
        job.lockedBy = null;
        job.lockedAt = null;
        if (job.retryCount >= maxRetries) {
            job.status = ProcessingJobStatus.FAILED;
            job.nextRetryAt = null;
        } else {
            job.status = ProcessingJobStatus.PENDING;
            job.nextRetryAt = OffsetDateTime.now().plus(retryDelay);
        }
        job.updatedAt = OffsetDateTime.now();
        processingJobRepository.save(job);
    }

    private ProcessingJob persistNewJob(String memoryId, String versionId, ProcessingJobType jobType) {
        ProcessingJob job = new ProcessingJob();
        job.id = UUID.randomUUID().toString();
        job.memoryId = memoryId;
        job.versionId = versionId;
        job.jobType = jobType;
        job.status = ProcessingJobStatus.PENDING;
        job.retryCount = 0;
        job.createdAt = OffsetDateTime.now();
        job.updatedAt = job.createdAt;
        try {
            return processingJobRepository.save(job);
        } catch (PersistenceException exception) {
            return processingJobRepository.findExisting(memoryId, versionId, jobType)
                    .orElseThrow(() -> exception);
        }
    }
}

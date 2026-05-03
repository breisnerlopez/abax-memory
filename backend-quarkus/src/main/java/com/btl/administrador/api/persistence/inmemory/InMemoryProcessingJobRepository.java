package com.btl.administrador.api.persistence.inmemory;

import com.btl.administrador.api.domain.ProcessingJob;
import com.btl.administrador.api.domain.ProcessingJobStatus;
import com.btl.administrador.api.domain.ProcessingJobType;
import com.btl.administrador.api.persistence.ProcessingJobRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryProcessingJobRepository implements ProcessingJobRepository {

    private final Map<String, ProcessingJob> jobs = new ConcurrentHashMap<>();

    @Override
    public ProcessingJob save(ProcessingJob processingJob) {
        jobs.put(processingJob.id, processingJob);
        return processingJob;
    }

    @Override
    public Optional<ProcessingJob> findExisting(String memoryId, String versionId, ProcessingJobType jobType) {
        return jobs.values().stream()
                .filter(job -> job.memoryId.equals(memoryId)
                        && job.versionId.equals(versionId)
                        && job.jobType == jobType)
                .findFirst();
    }

    @Override
    public List<ProcessingJob> findByStatus(ProcessingJobStatus status) {
        List<ProcessingJob> result = new ArrayList<>();
        for (ProcessingJob job : jobs.values()) {
            if (job.status == status) {
                result.add(job);
            }
        }
        return result;
    }

    @Override
    public List<ProcessingJob> claimPendingJobs(String workerId, int batchSize, OffsetDateTime now) {
        List<ProcessingJob> claimed = new ArrayList<>();
        for (ProcessingJob job : jobs.values()) {
            if (claimed.size() >= batchSize) {
                break;
            }
            boolean due = job.nextAttemptAt == null || !job.nextAttemptAt.isAfter(now);
            if (job.status == ProcessingJobStatus.PENDING && due) {
                job.status = ProcessingJobStatus.IN_PROGRESS;
                job.lockedBy = workerId;
                job.lockedAt = now;
                job.updatedAt = now;
                claimed.add(job);
            }
        }
        return claimed;
    }

    @Override
    public Optional<ProcessingJob> findById(String id) {
        return Optional.ofNullable(jobs.get(id));
    }

    @Override
    public void clear() {
        jobs.clear();
    }
}

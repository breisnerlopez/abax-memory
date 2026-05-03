package com.btl.administrador.api.persistence;

import com.btl.administrador.api.domain.ProcessingJob;
import com.btl.administrador.api.domain.ProcessingJobStatus;
import com.btl.administrador.api.domain.ProcessingJobType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ProcessingJobRepository {
    ProcessingJob save(ProcessingJob processingJob);

    Optional<ProcessingJob> findExisting(String memoryId, String versionId, ProcessingJobType jobType);

    List<ProcessingJob> findByStatus(ProcessingJobStatus status);

    List<ProcessingJob> claimPendingJobs(String workerId, int batchSize, OffsetDateTime now);

    Optional<ProcessingJob> findById(String id);

    void clear();
}

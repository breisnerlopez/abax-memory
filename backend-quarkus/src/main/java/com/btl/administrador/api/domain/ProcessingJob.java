package com.btl.administrador.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "processing_jobs")
public class ProcessingJob {

    @Id
    @Column(length = 36, nullable = false)
    public String id;

    @Column(name = "memory_id", nullable = false, length = 32)
    public String memoryId;

    @Column(name = "version_id", nullable = false, length = 36)
    public String versionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 40)
    public ProcessingJobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public ProcessingJobStatus status;

    @Column(name = "retry_count", nullable = false)
    public int retryCount;

    @Column(name = "last_error", columnDefinition = "TEXT")
    public String lastError;

    @Column(name = "locked_by", length = 120)
    public String lockedBy;

    @Column(name = "locked_at")
    public OffsetDateTime lockedAt;

    @Column(name = "next_retry_at")
    public OffsetDateTime nextRetryAt;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    public OffsetDateTime updatedAt;
}

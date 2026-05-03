package com.btl.administrador.api.service;

import com.btl.administrador.api.domain.ProcessingJobStatus;
import com.btl.administrador.api.domain.ProcessingJobType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessingJobServiceTest {

    @Test
    void createIfAbsent_sameMemoryVersionAndType_reusesExistingJob() {
        ServiceTestSupport support = new ServiceTestSupport();

        var created = support.processingJobService.createIfAbsent("MEM-001", "VER-001", ProcessingJobType.INDEX_MEMORY);
        var reused = support.processingJobService.createIfAbsent("MEM-001", "VER-001", ProcessingJobType.INDEX_MEMORY);

        assertThat(reused.id).isEqualTo(created.id);
        assertThat(support.processingJobRepository.findByStatus(ProcessingJobStatus.PENDING)).singleElement()
                .isSameAs(created);
    }

    @Test
    void lifecycleTransitions_updateStatusTimestampRetryAndError() {
        ServiceTestSupport support = new ServiceTestSupport();

        var job = support.processingJobService.createIfAbsent("MEM-001", "VER-001", ProcessingJobType.INDEX_MEMORY);
        var createdAt = job.createdAt;
        var claimedJobs = support.processingJobService.claimPendingJobs("worker-test-001", 10);

        assertThat(claimedJobs).singleElement().isSameAs(job);
        assertThat(job.status).isEqualTo(ProcessingJobStatus.IN_PROGRESS);
        assertThat(job.lockedBy).isEqualTo("worker-test-001");
        assertThat(job.lockedAt).isNotNull();
        assertThat(job.updatedAt).isAfterOrEqualTo(createdAt);

        support.processingJobService.markFailed(job, "index-timeout");

        assertThat(job.status).isEqualTo(ProcessingJobStatus.PENDING);
        assertThat(job.retryCount).isEqualTo(1);
        assertThat(job.lastError).isEqualTo("index-timeout");
        assertThat(job.lockedBy).isNull();
        assertThat(job.lockedAt).isNull();
        assertThat(job.nextAttemptAt).isNotNull();

        var reclaimedJobs = support.processingJobService.claimPendingJobs("worker-test-001", 10);

        assertThat(reclaimedJobs).singleElement().isSameAs(job);
        support.processingJobService.markCompleted(job);

        assertThat(job.status).isEqualTo(ProcessingJobStatus.COMPLETED);
        assertThat(job.lockedBy).isNull();
        assertThat(job.lockedAt).isNull();
        assertThat(job.nextAttemptAt).isNull();
        assertThat(job.lastError).isNull();
        assertThat(support.processingJobRepository.findByStatus(ProcessingJobStatus.COMPLETED)).singleElement()
                .isSameAs(job);
    }
}

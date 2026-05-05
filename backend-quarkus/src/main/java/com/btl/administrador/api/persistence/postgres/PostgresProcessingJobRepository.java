package com.btl.administrador.api.persistence.postgres;

import com.btl.administrador.api.domain.ProcessingJob;
import com.btl.administrador.api.domain.ProcessingJobStatus;
import com.btl.administrador.api.domain.ProcessingJobType;
import com.btl.administrador.api.persistence.ProcessingJobRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PostgresProcessingJobRepository implements ProcessingJobRepository {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    @Transactional
    public ProcessingJob save(ProcessingJob processingJob) {
        return entityManager.merge(processingJob);
    }

    @Override
    public Optional<ProcessingJob> findExisting(String memoryId, String versionId, ProcessingJobType jobType) {
        return entityManager.createQuery(
                        "FROM ProcessingJob job WHERE job.memoryId = :memoryId AND job.versionId = :versionId AND job.jobType = :jobType",
                        ProcessingJob.class)
                .setParameter("memoryId", memoryId)
                .setParameter("versionId", versionId)
                .setParameter("jobType", jobType)
                .getResultStream()
                .findFirst();
    }

    @Override
    public List<ProcessingJob> findByStatus(ProcessingJobStatus status) {
        return entityManager.createQuery(
                        "FROM ProcessingJob job WHERE job.status = :status ORDER BY job.createdAt ASC",
                        ProcessingJob.class)
                .setParameter("status", status)
                .getResultList();
    }

    @Override
    @Transactional
    public List<ProcessingJob> claimPendingJobs(String workerId, int batchSize, OffsetDateTime now) {
        @SuppressWarnings("unchecked")
        List<String> ids = entityManager.createNativeQuery(
                        "SELECT id FROM processing_jobs WHERE status = 'PENDING' "
                                + "AND (next_retry_at IS NULL OR next_retry_at <= ?1) "
                                + "ORDER BY created_at FOR UPDATE SKIP LOCKED")
                .setParameter(1, now)
                .setMaxResults(batchSize)
                .getResultList();

        if (ids.isEmpty()) {
            return List.of();
        }

        entityManager.createQuery(
                        "UPDATE ProcessingJob job SET job.status = :status, job.lockedBy = :workerId, "
                                + "job.lockedAt = :now, job.updatedAt = :now WHERE job.id IN :ids")
                .setParameter("status", ProcessingJobStatus.IN_PROGRESS)
                .setParameter("workerId", workerId)
                .setParameter("now", now)
                .setParameter("ids", ids)
                .executeUpdate();

        return entityManager.createQuery(
                        "FROM ProcessingJob job WHERE job.id IN :ids ORDER BY job.createdAt ASC",
                        ProcessingJob.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    @Override
    public Optional<ProcessingJob> findById(String id) {
        return Optional.ofNullable(entityManager.find(ProcessingJob.class, id));
    }

    @Override
    @Transactional
    public void clear() {
        entityManager.createQuery("DELETE FROM ProcessingJob").executeUpdate();
    }
}

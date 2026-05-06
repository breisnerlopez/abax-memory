-- ============================================================
-- Abax-Memory v2.0.0 — Migration V7 (H2-compatible)
-- Replaces the v1 processing_jobs table with the v2 schema.
-- H2-compatible version: uses VARCHAR(36) for id (matching the
-- ProcessingJob v1 entity) instead of UUID. Includes all v1
-- entity columns + v2 columns (payload, attempt, max_attempts,
-- error_message) as nullable extras.
-- PostgreSQL 16+ for production; H2 PostgreSQL mode for tests.
-- ============================================================

DROP TABLE IF EXISTS processing_jobs CASCADE;

CREATE TABLE processing_jobs (
    -- H2-compatible: VARCHAR(36) instead of UUID (matches ProcessingJob entity @Column(length=36))
    id              VARCHAR(36)     PRIMARY KEY,
    -- H2-compatible: VARCHAR(32) instead of UUID (matches entity @Column(length=32))
    memory_id       VARCHAR(32)     NOT NULL,
    -- v1 column: required by ProcessingJob entity
    version_id      VARCHAR(36),
    job_type        VARCHAR(40)     NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    -- v1 columns: required by ProcessingJob entity and JPQL queries
    retry_count     INTEGER         NOT NULL DEFAULT 0,
    last_error      TEXT,
    locked_by       VARCHAR(120),
    locked_at       TIMESTAMPTZ,
    next_retry_at   TIMESTAMPTZ,
    -- v2 columns: added as nullable for forward compatibility
    payload         TEXT,
    attempt         INTEGER         NOT NULL DEFAULT 0,
    max_attempts    INTEGER         NOT NULL DEFAULT 3,
    error_message   TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_jobs_type CHECK (
        job_type IN (
            'INDEX_MEMORY', 'RECONCILE_MEMORY', 'REINDEX_TENANT',
            'EXTRACT_ENTITIES'
        )
    ),
    CONSTRAINT chk_jobs_status CHECK (
        status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED')
    )
);

-- H2-compatible: removed WHERE clause (partial indexes not supported by H2)
CREATE INDEX idx_jobs_status_next_retry
    ON processing_jobs (status, next_retry_at);

CREATE INDEX idx_jobs_memory_id
    ON processing_jobs (memory_id);

-- ============================================================
-- Abax-Memory v2.0.0 — Migration V7
-- Replaces the v1 processing_jobs table with the v2 schema:
-- async job queue for embedding generation, Qdrant indexing,
-- and reconciliation. Drops v1 table (VARCHAR IDs, different
-- columns) before recreating with v2 schema (UUID, JSONB payload).
-- PostgreSQL 16+
-- ============================================================

DROP TABLE IF EXISTS processing_jobs CASCADE;

CREATE TABLE processing_jobs (
    id              UUID            PRIMARY KEY,
    memory_id       UUID            NOT NULL,
    job_type        VARCHAR(30)     NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    payload         JSONB,
    attempt         INTEGER         NOT NULL DEFAULT 0,
    max_attempts    INTEGER         NOT NULL DEFAULT 3,
    next_retry_at   TIMESTAMPTZ,
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
        status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')
    )
);

CREATE INDEX idx_jobs_status_next_retry
    ON processing_jobs (status, next_retry_at)
    WHERE status IN ('PENDING', 'FAILED');

CREATE INDEX idx_jobs_memory_id
    ON processing_jobs (memory_id);

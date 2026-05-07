-- ============================================================
-- Abax-Memory v2.1.0 — Migration V14
-- Adds weight, metadata, and updated_at columns to the
-- relations table for CP-V21-024 (Relation modification API).
--
-- ⚠ PENDING DBA REVIEW — Do not deploy without DBA approval.
-- PostgreSQL 16+
-- ============================================================

-- Add weight column (default 1.0 for existing rows)
ALTER TABLE relations
    ADD COLUMN IF NOT EXISTS weight DOUBLE PRECISION NOT NULL DEFAULT 1.0;

-- Add metadata column (JSONB for extensible key-value storage)
ALTER TABLE relations
    ADD COLUMN IF NOT EXISTS metadata JSONB NOT NULL DEFAULT '{}';

-- Add updated_at column (tracks last modification timestamp)
ALTER TABLE relations
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

-- Add index on updated_at for efficient time-range queries
CREATE INDEX IF NOT EXISTS idx_relations_updated_at
    ON relations (updated_at DESC);

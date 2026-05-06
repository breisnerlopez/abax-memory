-- ============================================================
-- Abax-Memory v2.0.0 — Migration V10
-- Feature 4: Hierarchical namespace support
-- Adds namespace column for scoped organizational hierarchies
-- PostgreSQL 16+
-- ============================================================

ALTER TABLE memory_fragments
    ADD COLUMN IF NOT EXISTS namespace VARCHAR(512);

-- Index for namespace-prefixed lookups
-- H2-compatible: removed WHERE clause (partial indexes not supported by H2)
CREATE INDEX IF NOT EXISTS idx_mem_frag_namespace
    ON memory_fragments (tenant_id, namespace);

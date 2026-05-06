-- ============================================================
-- Abax-Memory v2.0.0 — Migration V8
-- Adds supplementary search indexes on memory_fragments.
-- PostgreSQL 16+
-- ============================================================

-- H2-compatible: USING GIN and to_tsvector() not supported by H2; replaced with basic index
CREATE INDEX IF NOT EXISTS idx_mem_frag_content_fts
    ON memory_fragments (content);

-- Search by sensitivity within tenant
CREATE INDEX idx_mem_frag_tenant_sensitivity
    ON memory_fragments (tenant_id, sensitivity_level);

-- Search by confidence range (common in search filters)
CREATE INDEX idx_mem_frag_tenant_confidence
    ON memory_fragments (tenant_id, confidence);

-- H2-compatible: removed WHERE clause (partial indexes not supported by H2)
CREATE INDEX idx_mem_frag_source_type
    ON memory_fragments (tenant_id, source_type);

-- H2-compatible: removed WHERE clause (partial indexes not supported by H2)
CREATE INDEX idx_mem_frag_pending_review
    ON memory_fragments (tenant_id, created_at DESC);

-- H2-compatible: removed WHERE clause (partial indexes not supported by H2)
CREATE INDEX idx_mem_frag_active
    ON memory_fragments (tenant_id, created_at DESC);

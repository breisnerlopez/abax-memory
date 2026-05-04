-- ============================================================
-- Abax-Memory v2.0.0 — Migration V8
-- Adds supplementary search indexes on memory_fragments.
-- PostgreSQL 16+
-- ============================================================

-- Full-text search on content column
CREATE INDEX idx_mem_frag_content_fts
    ON memory_fragments USING GIN (to_tsvector('english', content));

-- Search by sensitivity within tenant
CREATE INDEX idx_mem_frag_tenant_sensitivity
    ON memory_fragments (tenant_id, sensitivity_level);

-- Search by confidence range (common in search filters)
CREATE INDEX idx_mem_frag_tenant_confidence
    ON memory_fragments (tenant_id, confidence);

-- Support index for source lookups
CREATE INDEX idx_mem_frag_source_type
    ON memory_fragments (tenant_id, source_type)
    WHERE source_type IS NOT NULL;

-- Support for listing pending reviews
CREATE INDEX idx_mem_frag_pending_review
    ON memory_fragments (tenant_id, created_at DESC)
    WHERE lifecycle_state = 'IN_REVIEW' AND deleted_at IS NULL;

-- Partial index for active (consumer-visible) memories
CREATE INDEX idx_mem_frag_active
    ON memory_fragments (tenant_id, created_at DESC)
    WHERE lifecycle_state = 'APPROVED' AND deleted_at IS NULL;

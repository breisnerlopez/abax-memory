-- ============================================================
-- Abax-Memory v2.0.0 — Migration V6
-- Creates the tenant_configs table: per-tenant limits, profile
-- assignment, and feature flags.
-- PostgreSQL 16+
-- ============================================================

CREATE TABLE tenant_configs (
    tenant_id               VARCHAR(100)    PRIMARY KEY,
    profile_id              UUID            REFERENCES profiles(id),
    rate_limit_per_min      INTEGER         NOT NULL DEFAULT 1000,
    rate_limit_user_per_min INTEGER         NOT NULL DEFAULT 300,
    max_top_k               INTEGER         NOT NULL DEFAULT 100,
    max_graph_depth         INTEGER         NOT NULL DEFAULT 5,
    max_batch_size          INTEGER         NOT NULL DEFAULT 100,
    max_memories            BIGINT,
    is_active               BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Abax-Memory v2.0.0 — Migration V5
-- Creates the profiles table: domain-profile configurations
-- stored as JSONB.
-- PostgreSQL 16+
-- ============================================================

CREATE TABLE profiles (
    id              UUID            PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL UNIQUE,
    version         VARCHAR(10)     NOT NULL DEFAULT '1.0',
    description     TEXT,
    config          JSONB           NOT NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_profiles_name ON profiles (name);
-- H2-compatible: removed WHERE clause (partial indexes not supported by H2)
CREATE INDEX idx_profiles_active ON profiles (is_active);

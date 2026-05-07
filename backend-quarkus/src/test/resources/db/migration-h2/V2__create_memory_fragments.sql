-- ============================================================
-- Abax-Memory v2.0.0 — Migration V2
-- Creates the central memory_fragments table.
-- PostgreSQL 16+
-- ============================================================

CREATE TABLE memory_fragments (
    id              UUID            PRIMARY KEY,
    tenant_id       VARCHAR(100)    NOT NULL,
    scope_id        VARCHAR(255),
    kind            VARCHAR(30)     NOT NULL,
    title           VARCHAR(500)    NOT NULL,
    content         TEXT            NOT NULL,
    summary         TEXT,
    lifecycle_state VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    sensitivity_level VARCHAR(20)   NOT NULL DEFAULT 'INTERNAL',
    source_type     VARCHAR(50),
    source_ref      VARCHAR(500),
    confidence      DOUBLE PRECISION NOT NULL DEFAULT 0.5,
    embedding_id    VARCHAR(255),
    reviewer_id     VARCHAR(255),
    review_comment  TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,

    -- ── Check constraints ──
    CONSTRAINT chk_mem_frag_kind CHECK (
        kind IN (
            'DECISION', 'INCIDENT', 'ENTITY', 'KNOWLEDGE',
            'FEATURE', 'AGENT_MEMORY', 'DOCUMENT', 'CUSTOM'
        )
    ),
    CONSTRAINT chk_mem_frag_lifecycle CHECK (
        lifecycle_state IN (
            'DRAFT', 'IN_REVIEW', 'APPROVED',
            'DEPRECATED', 'ARCHIVED', 'DELETED'
        )
    ),
    CONSTRAINT chk_mem_frag_sensitivity CHECK (
        sensitivity_level IN (
            'PUBLIC', 'INTERNAL', 'CONFIDENTIAL', 'RESTRICTED'
        )
    ),
    CONSTRAINT chk_mem_frag_confidence_range CHECK (
        confidence >= 0.0 AND confidence <= 1.0
    )
);

-- ── Initial indexes ──
CREATE INDEX idx_mem_frag_tenant
    ON memory_fragments (tenant_id);

CREATE INDEX idx_mem_frag_tenant_state
    ON memory_fragments (tenant_id, lifecycle_state);

CREATE INDEX idx_mem_frag_tenant_kind
    ON memory_fragments (tenant_id, kind);

-- H2-compatible: removed WHERE clause (partial indexes not supported by H2)
CREATE INDEX idx_mem_frag_tenant_not_deleted
    ON memory_fragments (tenant_id, created_at DESC);

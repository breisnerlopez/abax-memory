-- ============================================================
-- Abax-Memory v2.0.0 — Migration V4
-- Creates the audit_records table: append-only, immutable log
-- of every mutation in the system.
-- PostgreSQL 16+
-- ============================================================

CREATE TABLE audit_records (
    id              UUID            PRIMARY KEY,
    memory_id       UUID            NOT NULL,
    tenant_id       VARCHAR(100)    NOT NULL,
    user_id         VARCHAR(255)    NOT NULL,
    action          VARCHAR(30)     NOT NULL,
    diff            JSONB           NOT NULL DEFAULT '{}',
    ip_address      VARCHAR(45),
    user_agent      TEXT,
    correlation_id  VARCHAR(64),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_audit_action CHECK (
        action IN (
            'CREATE', 'UPDATE', 'REVIEW_APPROVE', 'REVIEW_REJECT',
            'REVIEW_RETURN', 'DEPRECATE', 'ARCHIVE', 'SOFT_DELETE',
            'RELATION_CREATE', 'RELATION_DELETE'
        )
    )
);

CREATE INDEX idx_audit_memory_id ON audit_records (memory_id);
CREATE INDEX idx_audit_tenant_id ON audit_records (tenant_id);
CREATE INDEX idx_audit_user_id ON audit_records (user_id);
CREATE INDEX idx_audit_action ON audit_records (action);
CREATE INDEX idx_audit_created_at ON audit_records (tenant_id, created_at DESC);

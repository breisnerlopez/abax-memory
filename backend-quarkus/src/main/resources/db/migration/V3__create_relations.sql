-- ============================================================
-- Abax-Memory v2.0.0 — Migration V3
-- Creates the relations table for typed, directed relationships
-- between memory fragments.
-- PostgreSQL 16+
-- ============================================================

CREATE TABLE relations (
    id              UUID            PRIMARY KEY,
    source_id       UUID            NOT NULL REFERENCES memory_fragments(id)
                                    ON DELETE CASCADE,
    target_id       UUID            NOT NULL REFERENCES memory_fragments(id)
                                    ON DELETE CASCADE,
    relation_type   VARCHAR(30)     NOT NULL,
    tenant_id       VARCHAR(100)    NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- No self-relations
    CONSTRAINT chk_relations_no_self CHECK (source_id <> target_id),

    -- No duplicate relations (same source + target + type)
    CONSTRAINT uq_relations UNIQUE (source_id, target_id, relation_type),

    -- Valid types
    CONSTRAINT chk_relations_type CHECK (
        relation_type IN (
            'RELATES_TO', 'DEPENDS_ON', 'BLOCKED_BY', 'RESOLVES',
            'SUPERSEDES', 'REFERENCES', 'DERIVES_FROM',
            'CONTRADICTS', 'SUPPORTS'
        )
    )
);

CREATE INDEX idx_relations_source ON relations (source_id);
CREATE INDEX idx_relations_target ON relations (target_id);
CREATE INDEX idx_relations_tenant ON relations (tenant_id);
CREATE INDEX idx_relations_type ON relations (source_id, relation_type);

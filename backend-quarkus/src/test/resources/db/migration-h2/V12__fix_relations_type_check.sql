-- ============================================================
-- Abax-Memory v2.0.0 — Migration V12
-- Fixes RelationType CHECK constraint in the relations table
-- to match the Java v2 enum values (BUG-003 / UAT-S04).
--
-- Background: V3 created the relations table with legacy v1
-- values in the CHECK constraint (RELATES_TO, BLOCKED_BY,
-- REFERENCES, DERIVES_FROM). The Java RelationType enum was
-- redesigned for v2 (RELATED_TO, CAUSED_BY, MENTIONS,
-- BELONGS_TO) but V11 only fixed constraints on
-- memory_fragments and audit_records — the relations table
-- was overlooked.
--
-- This migration:
--   1. Drops the old chk_relations_type constraint
--   2. Creates a new chk_relations_type constraint with
--      the correct Java v2 RelationType enum values
-- ============================================================

-- ── Fix RelationType CHECK constraint ────────────────────────

ALTER TABLE relations
    DROP CONSTRAINT IF EXISTS chk_relations_type;

ALTER TABLE relations
    ADD CONSTRAINT chk_relations_type CHECK (
        relation_type IN (
            'RELATED_TO',    -- was: RELATES_TO in v1
            'DEPENDS_ON',    -- same in both versions
            'CAUSED_BY',     -- was: BLOCKED_BY in v1
            'RESOLVES',      -- same in both versions
            'CONTRADICTS',   -- same in both versions
            'SUPPORTS',      -- same in both versions
            'MENTIONS',      -- was: REFERENCES in v1
            'BELONGS_TO',    -- was: DERIVES_FROM in v1
            'SUPERSEDES'     -- same in both versions
        )
    );

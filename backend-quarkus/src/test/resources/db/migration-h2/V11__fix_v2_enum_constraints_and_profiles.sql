-- ============================================================
-- Abax-Memory v2.0.0 — Migration V11
-- Fixes enum mismatches between Java v2 enums and DB CHECK
-- constraints (BUG-001, BUG-002) and updates seed profile
-- configs to reference valid v2 MemoryKind and SensitivityLevel
-- values.
--
-- Background: V2 migration was created with legacy v1 enum
-- values in CHECK constraints. The Java domain enums were
-- re-designed for v2 (new values, semantic naming) but the
-- DB constraints were never synchronized.
--
-- This migration:
--   1. Drops old CHECK constraints with v1 values
--   2. Creates new CHECK constraints with v2 values
--   3. Updates profile config JSONB to use valid v2 enums
--   4. Fixes audit_records action CHECK constraint (BUG-002c)
-- ============================================================

-- ── 1. Fix MemoryKind CHECK constraint (BUG-001) ─────────────

ALTER TABLE memory_fragments
    DROP CONSTRAINT IF EXISTS chk_mem_frag_kind;

ALTER TABLE memory_fragments
    ADD CONSTRAINT chk_mem_frag_kind CHECK (
        kind IN (
            'FACT',        -- was: not present in v1
            'PREFERENCE',  -- was: not present in v1
            'EVENT',       -- was: INCIDENT in v1
            'DECISION',    -- same in v1
            'TASK',        -- was: not present in v1
            'PROCEDURE',   -- was: not present in v1
            'NOTE',        -- was: KNOWLEDGE/CUSTOM in v1
            'ENTITY'       -- same in v1
        )
    );

-- ── 2. Fix LifecycleState CHECK constraint (BUG-002a) ────────

ALTER TABLE memory_fragments
    DROP CONSTRAINT IF EXISTS chk_mem_frag_lifecycle;

ALTER TABLE memory_fragments
    ADD CONSTRAINT chk_mem_frag_lifecycle CHECK (
        lifecycle_state IN (
            'DRAFT',       -- same in v1
            'PENDING',     -- was: IN_REVIEW in v1
            'ACTIVE',      -- was: APPROVED in v1
            'REJECTED',    -- was: not present in v1
            'ARCHIVED',    -- same in v1
            'DELETED'      -- same in v1
        )
    );

-- ── 3. Fix SensitivityLevel CHECK constraint (BUG-002b) ──────

ALTER TABLE memory_fragments
    DROP CONSTRAINT IF EXISTS chk_mem_frag_sensitivity;

ALTER TABLE memory_fragments
    ADD CONSTRAINT chk_mem_frag_sensitivity CHECK (
        sensitivity_level IN (
            'PUBLIC',       -- same in v1
            'INTERNAL',     -- same in v1
            'CONFIDENTIAL', -- same in v1
            'SECRET'        -- was: RESTRICTED in v1
        )
    );

-- ── 4. Fix seed profile configs ──────────────────────────────
-- V9 seeded profiles with legacy enum values. Updates replace
-- invalid v1 values with their closest v2 equivalents.
-- Each REPLACE wraps ::jsonb → cast to text → replace → cast
-- back to jsonb, ensuring valid JSON output.

-- 4a. Ops profile: INCIDENT→EVENT, KNOWLEDGE→NOTE, RESTRICTED→SECRET
UPDATE profiles
SET config = REPLACE(
                REPLACE(
                    REPLACE(config::text,
                        '"INCIDENT"', '"EVENT"'),
                    '"KNOWLEDGE"', '"NOTE"'),
                '"RESTRICTED"', '"SECRET"')::jsonb
WHERE id = 'b0000000-0000-0000-0000-000000000001'
  AND config::text LIKE '%"INCIDENT"%';

-- 4b. Agent profile: AGENT_MEMORY→FACT, KNOWLEDGE→NOTE, RESTRICTED→SECRET
UPDATE profiles
SET config = REPLACE(
                REPLACE(
                    REPLACE(config::text,
                        '"AGENT_MEMORY"', '"FACT"'),
                    '"KNOWLEDGE"', '"NOTE"'),
                '"RESTRICTED"', '"SECRET"')::jsonb
WHERE id = 'b0000000-0000-0000-0000-000000000002'
  AND config::text LIKE '%"AGENT_MEMORY"%';

-- 4c. Business profile: DOCUMENT→PROCEDURE, CUSTOM→NOTE,
--     KNOWLEDGE→NOTE, RESTRICTED→SECRET
UPDATE profiles
SET config = REPLACE(
                REPLACE(
                    REPLACE(
                        REPLACE(config::text,
                            '"DOCUMENT"', '"PROCEDURE"'),
                        '"CUSTOM"', '"NOTE"'),
                    '"KNOWLEDGE"', '"NOTE"'),
                '"RESTRICTED"', '"SECRET"')::jsonb
WHERE id = 'b0000000-0000-0000-0000-000000000003'
  AND config::text LIKE '%"DOCUMENT"%';

-- 4d. Catch-all: any remaining RESTRICTED → SECRET in any profile
UPDATE profiles
SET config = REPLACE(config::text, '"RESTRICTED"', '"SECRET"')::jsonb
WHERE config::text LIKE '%"RESTRICTED"%';

-- ── 5. Fix audit action CHECK constraint (BUG-002c) ──────────
-- V4 constraint only allowed legacy v1 actions (REVIEW_APPROVE,
-- REVIEW_REJECT, DEPRECATE). v2 MemoryServiceImpl uses
-- REVIEW_REQUESTED, REVIEWED, REVIEW_REJECTED, LIFECYCLE_CHANGED.

ALTER TABLE audit_records
    DROP CONSTRAINT IF EXISTS chk_audit_action;

ALTER TABLE audit_records
    ADD CONSTRAINT chk_audit_action CHECK (
        action IN (
            'CREATE',              -- same in v1
            'UPDATE',              -- same in v1
            'REVIEW_REQUESTED',    -- was: not present in v1
            'REVIEWED',            -- was: REVIEW_APPROVE in v1
            'REVIEW_REJECTED',     -- was: REVIEW_REJECT in v1
            'REVIEW_RETURN',       -- same in v1
            'ARCHIVE',             -- same in v1
            'SOFT_DELETE',         -- same in v1
            'RELATION_CREATE',     -- same in v1
            'RELATION_DELETE',     -- same in v1
            'LIFECYCLE_CHANGED'    -- was: not present in v1
        )
    );

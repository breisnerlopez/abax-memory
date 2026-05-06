-- ============================================================
-- Abax-Memory v2.1.0 — Migration V13
-- Adds NAMESPACE_DELETE to the chk_audit_action constraint
-- on the audit_records table.
--
-- Background: FT-V21-004.3 introduced the DELETE namespace
-- endpoint which records audit entries with action
-- 'NAMESPACE_DELETE'. V11 did not include this value in the
-- constraint, causing ConstraintViolationException when the
-- endpoint was exercised.
--
-- Defect: DEF-V21-008
-- ============================================================

-- Drop the existing constraint (created in V4, updated in V11)
ALTER TABLE audit_records
    DROP CONSTRAINT IF EXISTS chk_audit_action;

-- Recreate with NAMESPACE_DELETE added
ALTER TABLE audit_records
    ADD CONSTRAINT chk_audit_action CHECK (
        action IN (
            'CREATE',
            'UPDATE',
            'REVIEW_REQUESTED',
            'REVIEWED',
            'REVIEW_REJECTED',
            'REVIEW_RETURN',
            'ARCHIVE',
            'SOFT_DELETE',
            'RELATION_CREATE',
            'RELATION_DELETE',
            'LIFECYCLE_CHANGED',
            'NAMESPACE_DELETE'
        )
    );

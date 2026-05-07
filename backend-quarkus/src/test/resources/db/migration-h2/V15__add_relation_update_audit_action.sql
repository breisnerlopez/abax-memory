-- ============================================================
-- Abax-Memory v2.1.0 — Migration V15 (H2)
-- Adds RELATION_UPDATE, DOMAIN_UPDATE, DOMAIN_DELETE, and
-- NAMESPACE_DELETE to the chk_audit_action constraint on
-- the audit_records table.
--
-- Note: H2 test migrations skip V13 (NAMESPACE_DELETE), so
-- this migration includes NAMESPACE_DELETE plus the three new
-- actions from DEF-V21-012.
--
-- Defect: DEF-V21-012
-- ============================================================

-- Drop the existing constraint (created in V4, updated in V11)
ALTER TABLE audit_records
    DROP CONSTRAINT IF EXISTS chk_audit_action;

-- Recreate with all v2.1.0 audit actions
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
            'NAMESPACE_DELETE',
            'RELATION_UPDATE',
            'DOMAIN_UPDATE',
            'DOMAIN_DELETE'
        )
    );

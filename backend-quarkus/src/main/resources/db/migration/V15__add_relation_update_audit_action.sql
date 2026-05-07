-- ============================================================
-- Abax-Memory v2.1.0 — Migration V15
-- Adds RELATION_UPDATE, DOMAIN_UPDATE, and DOMAIN_DELETE to
-- the chk_audit_action constraint on the audit_records table.
--
-- Background: FT-V21-024 (CP-V21-024) introduced PUT/PATCH
-- endpoints for relations that record audit entries with action
-- 'RELATION_UPDATE'. V13 added NAMESPACE_DELETE but overlooked
-- RELATION_UPDATE, DOMAIN_UPDATE, and DOMAIN_DELETE.
--
-- Defect: DEF-V21-012
-- ============================================================

-- Drop the existing constraint (created in V4, updated in V11/V13)
ALTER TABLE audit_records
    DROP CONSTRAINT IF EXISTS chk_audit_action;

-- Recreate with RELATION_UPDATE, DOMAIN_UPDATE, DOMAIN_DELETE added
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

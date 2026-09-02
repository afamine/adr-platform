ALTER TABLE workspace_membership
    DROP CONSTRAINT IF EXISTS chk_workspace_membership_status;

ALTER TABLE workspace_membership
    ADD CONSTRAINT chk_workspace_membership_status
    CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED'));

-- V21: Registration now creates a brand-new workspace per user.
--
-- Previously, new accounts were assigned to the hardcoded "default" workspace
-- (seeded in V1 + V6). From this version onward, every self-registration call
-- creates its own workspace and the registering user becomes its ADMIN.
--
-- Invited users are unaffected: the invite flow (AcceptInviteRequest) continues
-- to join an existing workspace; no changes are made to the invite tables.
--
-- The V6 "default" workspace and its seed admin are intentionally kept for
-- local development and first-boot convenience. They are not removed here.

-- Ensure workspace_id on users has NO default expression
-- (it was never defined with a DEFAULT in V1, but this guard makes the intent explicit).
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM   information_schema.columns
        WHERE  table_name   = 'users'
        AND    column_name  = 'workspace_id'
        AND    column_default IS NOT NULL
    ) THEN
        EXECUTE 'ALTER TABLE users ALTER COLUMN workspace_id DROP DEFAULT';
    END IF;
END $$;

-- Index to speed up workspace-scoped user queries (safe no-op if already present).
CREATE INDEX IF NOT EXISTS idx_users_workspace_created ON users(workspace_id, created_at);

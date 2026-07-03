DO $$
BEGIN
    ALTER TABLE workspace
      ADD COLUMN IF NOT EXISTS join_policy VARCHAR(30) NOT NULL DEFAULT 'INVITE_ONLY';

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_name = 'workspace'
          AND constraint_name = 'chk_workspace_join_policy'
    ) THEN
        ALTER TABLE workspace
          ADD CONSTRAINT chk_workspace_join_policy
          CHECK (join_policy IN ('INVITE_ONLY', 'ALLOW_SLUG', 'CLOSED'));
    END IF;
END $$;

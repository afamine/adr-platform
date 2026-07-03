CREATE TABLE IF NOT EXISTS workspace_membership (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    workspace_id UUID NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    role VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    accepted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_workspace_membership_user_workspace UNIQUE (user_id, workspace_id),
    CONSTRAINT chk_workspace_membership_status CHECK (status IN ('PENDING', 'ACTIVE'))
);

INSERT INTO workspace_membership (user_id, workspace_id, role, status, created_at, accepted_at)
SELECT id, workspace_id, role, 'ACTIVE', created_at,
       CASE WHEN is_active = TRUE THEN created_at ELSE NULL END
FROM users
ON CONFLICT (user_id, workspace_id) DO NOTHING;

ALTER TABLE verification_token
    ADD COLUMN IF NOT EXISTS workspace_id UUID REFERENCES workspace(id);

CREATE INDEX IF NOT EXISTS idx_workspace_membership_user_status ON workspace_membership(user_id, status);
CREATE INDEX IF NOT EXISTS idx_workspace_membership_workspace_status ON workspace_membership(workspace_id, status);
CREATE INDEX IF NOT EXISTS idx_verification_token_workspace_type ON verification_token(workspace_id, token_type, used, expires_at);

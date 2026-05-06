CREATE TABLE vote (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    adr_id          UUID NOT NULL REFERENCES adr(id) ON DELETE CASCADE,
    workspace_id    UUID NOT NULL,
    voter_id        UUID NOT NULL REFERENCES users(id),
    vote_type       VARCHAR(10) NOT NULL CHECK (vote_type IN ('APPROVE','REJECT')),
    comment         TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(adr_id, voter_id)
);

CREATE INDEX idx_vote_adr ON vote(adr_id);
CREATE INDEX idx_vote_workspace ON vote(workspace_id, adr_id);

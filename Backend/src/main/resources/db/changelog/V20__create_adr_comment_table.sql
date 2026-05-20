CREATE TABLE adr_comment (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    adr_id          UUID        NOT NULL REFERENCES adr(id) ON DELETE CASCADE,
    workspace_id    UUID        NOT NULL,
    author_id       UUID        NOT NULL REFERENCES users(id),
    content         TEXT        NOT NULL,
    is_resolved     BOOLEAN     NOT NULL DEFAULT FALSE,
    resolved_by     UUID        REFERENCES users(id),
    resolved_at     TIMESTAMP,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comment_adr       ON adr_comment(adr_id, created_at DESC);
CREATE INDEX idx_comment_workspace ON adr_comment(workspace_id);

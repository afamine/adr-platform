CREATE TABLE IF NOT EXISTS notification (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id    UUID NOT NULL,
    recipient_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    audit_event_id  UUID REFERENCES audit_event(id) ON DELETE SET NULL,
    type            VARCHAR(30) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    body            TEXT NOT NULL,
    adr_id          UUID REFERENCES adr(id) ON DELETE CASCADE,
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notif_recipient ON notification(recipient_id, is_read, created_at DESC);
CREATE INDEX idx_notif_workspace  ON notification(workspace_id, created_at DESC);

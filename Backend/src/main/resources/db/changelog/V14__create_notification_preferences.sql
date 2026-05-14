CREATE TABLE IF NOT EXISTS notification_preferences (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    email_on_review BOOLEAN NOT NULL DEFAULT TRUE,
    email_on_vote   BOOLEAN NOT NULL DEFAULT TRUE,
    email_on_status BOOLEAN NOT NULL DEFAULT TRUE,
    slack_enabled   BOOLEAN NOT NULL DEFAULT FALSE,
    slack_webhook   VARCHAR(500),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO notification_preferences (user_id)
SELECT id FROM users
ON CONFLICT (user_id) DO NOTHING;

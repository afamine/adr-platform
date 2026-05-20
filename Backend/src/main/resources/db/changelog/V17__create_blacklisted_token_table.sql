CREATE TABLE IF NOT EXISTS blacklisted_token (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_blacklisted_token_hash     ON blacklisted_token (token_hash);
CREATE INDEX IF NOT EXISTS idx_blacklisted_token_expires  ON blacklisted_token (expires_at);

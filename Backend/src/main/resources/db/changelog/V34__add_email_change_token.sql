ALTER TABLE verification_token
    ADD COLUMN IF NOT EXISTS pending_email VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_vtoken_email_change
    ON verification_token(user_id, token_type, used, expires_at)
    WHERE token_type = 'EMAIL_CHANGE';

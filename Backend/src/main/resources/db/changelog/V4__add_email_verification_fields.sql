ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT FALSE;

-- Activate every existing account so live users are not locked out by this migration
UPDATE users SET email_verified = TRUE, is_active = TRUE
WHERE created_at IS NOT NULL;

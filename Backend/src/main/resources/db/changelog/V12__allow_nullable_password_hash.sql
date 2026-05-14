-- Invited users have no password until they accept the invitation
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

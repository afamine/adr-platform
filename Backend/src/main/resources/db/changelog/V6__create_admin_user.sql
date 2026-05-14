-- Initial platform administrator account.
--
-- Default admin:  admin@adrmanager.com  /  CHANGE PASSWORD IMMEDIATELY AFTER FIRST LOGIN
-- Role: ADMIN  |  email_verified=true  |  is_active=true
--
-- Password hash: BCrypt strength=12. Replace with a new hash before deploying to any
-- shared or production environment.

INSERT INTO users (
    id,
    workspace_id,
    email,
    password_hash,
    full_name,
    role,
    email_verified,
    is_active,
    created_at
) VALUES (
    gen_random_uuid(),
    (SELECT id FROM workspace WHERE slug = 'default' LIMIT 1),
    'admin@adrmanager.com',
    '$2a$12$X.wrZcTvPLy.jliC2ie9Au8VmWQxUElZ9GfyYI9L914qZj15VjcJm',
    'Platform Admin',
    'ADMIN',
    TRUE,
    TRUE,
    NOW()
)
ON CONFLICT (email) DO NOTHING;

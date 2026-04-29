-- Initial platform administrator account.
--
-- Default admin credentials (CHANGE THE PASSWORD AFTER FIRST LOGIN):
--   email    : admin@adrmanager.com
--   password : Admin@1234
--   role     : ADMIN
--   status   : email_verified=true, is_active=true
--
-- Hash below is BCrypt strength=12 of "Admin@1234" generated with
-- new BCryptPasswordEncoder(12).encode("Admin@1234")
-- BCrypt salts are random, so the verification side regenerates the salt from
-- the stored hash; any other strength-12 hash of the same password would also work.

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

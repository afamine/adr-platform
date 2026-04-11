DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_available_extensions WHERE name = 'vector') THEN
        CREATE EXTENSION IF NOT EXISTS vector;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS workspace (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspace(id),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_workspace_id ON users(workspace_id);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector') THEN
        EXECUTE 'CREATE TABLE IF NOT EXISTS adr (
            id UUID PRIMARY KEY,
            workspace_id UUID NOT NULL REFERENCES workspace(id),
            title VARCHAR(255) NOT NULL,
            embedding_vector vector(1536),
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        )';
    ELSE
        EXECUTE 'CREATE TABLE IF NOT EXISTS adr (
            id UUID PRIMARY KEY,
            workspace_id UUID NOT NULL REFERENCES workspace(id),
            title VARCHAR(255) NOT NULL,
            embedding_vector TEXT,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        )';
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_adr_workspace_id ON adr(workspace_id);

CREATE TABLE IF NOT EXISTS audit_event (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspace(id),
    user_id UUID REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    old_value_json TEXT,
    new_value_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_workspace_id ON audit_event(workspace_id);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_event(entity_type, entity_id);

INSERT INTO workspace (id, name, slug, created_at)
VALUES ('00000000-0000-0000-0000-000000000001', 'Default Workspace', 'default', CURRENT_TIMESTAMP)
ON CONFLICT (slug) DO NOTHING;

CREATE TABLE IF NOT EXISTS projects (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspace(id),
    name VARCHAR(120) NOT NULL,
    description TEXT,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_projects_workspace_name UNIQUE (workspace_id, name)
);

ALTER TABLE adr ADD COLUMN IF NOT EXISTS project_id UUID REFERENCES projects(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_projects_workspace_active ON projects(workspace_id, archived, name);
CREATE INDEX IF NOT EXISTS idx_adr_workspace_project ON adr(workspace_id, project_id);

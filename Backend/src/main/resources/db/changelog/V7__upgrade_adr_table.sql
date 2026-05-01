-- Upgrade ADR table to full ADR model and add helper function

-- Add new columns if they don't exist
ALTER TABLE adr ADD COLUMN IF NOT EXISTS adr_number INTEGER;
ALTER TABLE adr ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','PROPOSED','UNDER_REVIEW','ACCEPTED','REJECTED','SUPERSEDED'));
ALTER TABLE adr ADD COLUMN IF NOT EXISTS context TEXT;
ALTER TABLE adr ADD COLUMN IF NOT EXISTS decision TEXT;
ALTER TABLE adr ADD COLUMN IF NOT EXISTS consequences TEXT;
ALTER TABLE adr ADD COLUMN IF NOT EXISTS alternatives TEXT;
-- Store tags as comma-separated TEXT to avoid driver extensions
ALTER TABLE adr ADD COLUMN IF NOT EXISTS tags TEXT;
ALTER TABLE adr ADD COLUMN IF NOT EXISTS author_id UUID;
ALTER TABLE adr ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();

-- Backfill adr_number for existing rows that don't have one
WITH numbered AS (
  SELECT id, ROW_NUMBER() OVER (PARTITION BY workspace_id ORDER BY created_at, id) AS rn
  FROM adr WHERE adr_number IS NULL
)
UPDATE adr a
SET adr_number = n.rn
FROM numbered n
WHERE a.id = n.id;

-- Backfill author_id with the first user in the same workspace if missing
UPDATE adr a
SET author_id = COALESCE(author_id, (
  SELECT u.id FROM users u
  WHERE u.workspace_id = a.workspace_id
  ORDER BY u.created_at
  LIMIT 1
))
WHERE a.author_id IS NULL;

-- Ensure author_id is NOT NULL and has FK
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE constraint_name = 'fk_adr_author' AND table_name = 'adr'
  ) THEN
    ALTER TABLE adr ADD CONSTRAINT fk_adr_author FOREIGN KEY (author_id) REFERENCES users(id);
  END IF;
END $$;

ALTER TABLE adr ALTER COLUMN author_id SET NOT NULL;

-- Unique constraint per workspace on adr_number
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'uk_adr_workspace_number'
  ) THEN
    ALTER TABLE adr ADD CONSTRAINT uk_adr_workspace_number UNIQUE (workspace_id, adr_number);
  END IF;
END $$;

-- Helpful indexes
CREATE INDEX IF NOT EXISTS idx_adr_workspace ON adr(workspace_id);
CREATE INDEX IF NOT EXISTS idx_adr_workspace_status ON adr(workspace_id, status);
CREATE INDEX IF NOT EXISTS idx_adr_workspace_number ON adr(workspace_id, adr_number);

-- Workspace-scoped next ADR number helper
CREATE OR REPLACE FUNCTION next_adr_number(p_workspace_id UUID)
RETURNS INTEGER AS $$
  SELECT COALESCE(MAX(adr_number), 0) + 1
  FROM adr
  WHERE workspace_id = p_workspace_id;
$$ LANGUAGE SQL;

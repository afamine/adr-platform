ALTER TABLE adr
    ADD COLUMN IF NOT EXISTS superseded_by_id UUID REFERENCES adr(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS supersedes_id     UUID REFERENCES adr(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_adr_superseded_by ON adr(superseded_by_id);
CREATE INDEX IF NOT EXISTS idx_adr_supersedes    ON adr(supersedes_id);

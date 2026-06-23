-- Enable trigram extension for GIN-based LIKE search acceleration.
-- pg_trgm makes '%keyword%' LIKE queries use an index instead of sequential scan.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Composite expression index covering all searchable text fields in one index.
-- PostgreSQL's query planner will use this for LIKE queries on the concatenation,
-- but per-column indexes are needed for the existing per-column LIKE conditions.
CREATE INDEX IF NOT EXISTS idx_adr_title_trgm
  ON adr USING gin(lower(title) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_adr_context_trgm
  ON adr USING gin(lower(context) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_adr_decision_trgm
  ON adr USING gin(lower(decision) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_adr_alternatives_trgm
  ON adr USING gin(lower(alternatives) gin_trgm_ops);

-- Optimize audit_event for ADR lookups without changing existing schema
CREATE INDEX IF NOT EXISTS idx_audit_workspace_created_desc ON audit_event(workspace_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_adr_entity ON audit_event(entity_id, created_at DESC) WHERE entity_type = 'ADR';

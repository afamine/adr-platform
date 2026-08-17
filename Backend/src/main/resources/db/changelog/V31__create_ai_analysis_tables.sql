CREATE TABLE ai_analysis_result (
    id UUID PRIMARY KEY,
    adr_id UUID NOT NULL REFERENCES adr(id) ON DELETE CASCADE,
    adr_version_hash VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    generated_at TIMESTAMPTZ,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_analysis_result_adr_created
    ON ai_analysis_result (adr_id, created_at DESC);

CREATE INDEX idx_ai_analysis_result_adr_version_status
    ON ai_analysis_result (adr_id, adr_version_hash, status);

CREATE TABLE ai_analysis_insight (
    id UUID PRIMARY KEY,
    analysis_id UUID NOT NULL REFERENCES ai_analysis_result(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    summary TEXT NOT NULL,
    impact VARCHAR(10) NOT NULL,
    confidence INTEGER NOT NULL CHECK (confidence >= 0 AND confidence <= 100),
    rationale TEXT NOT NULL,
    source_reference VARCHAR(32) NOT NULL,
    source_quote TEXT
);

CREATE INDEX idx_ai_analysis_insight_analysis
    ON ai_analysis_insight (analysis_id);

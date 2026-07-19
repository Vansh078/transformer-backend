-- Report Management module: metadata for every generated report.
-- Only the Supabase Storage object path is stored — signed URLs are re-issued on demand
-- since they expire and must never be persisted.
CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    transformer_id BIGINT REFERENCES transformers(id) ON DELETE CASCADE,
    report_type VARCHAR(20) NOT NULL, -- MANUAL, DAILY, WEEKLY, MONTHLY, CRITICAL
    report_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL,
    generated_by VARCHAR(150),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, GENERATED, FAILED, ARCHIVED
    file_size_bytes BIGINT,
    failure_reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_reports_transformer ON reports (transformer_id, generated_at);
CREATE INDEX idx_reports_type ON reports (report_type, generated_at);
CREATE INDEX idx_reports_status ON reports (status);

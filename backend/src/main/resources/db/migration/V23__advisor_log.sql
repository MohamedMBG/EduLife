CREATE TABLE advisor_log (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    goal TEXT NOT NULL,
    response_json JSONB NOT NULL,
    provider VARCHAR(32) NOT NULL,
    model VARCHAR(64) NOT NULL,
    latency_ms INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_advisor_log_user_created
ON advisor_log(user_id, created_at DESC);

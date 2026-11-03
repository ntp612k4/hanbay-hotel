CREATE TABLE ai_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    feature VARCHAR(50) NOT NULL,
    prompt TEXT NOT NULL,
    response TEXT,
    error_message TEXT,
    successful BOOLEAN NOT NULL,
    duration_ms BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_requests_user_created_at ON ai_requests(user_id, created_at DESC);
CREATE INDEX idx_ai_requests_feature_created_at ON ai_requests(feature, created_at DESC);

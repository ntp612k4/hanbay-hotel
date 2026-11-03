CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expired_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    CONSTRAINT chk_password_reset_token_lifecycle
        CHECK (
            used_at IS NULL
            OR revoked_at IS NULL
            OR used_at <= revoked_at
        )
);

CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_expired_at ON password_reset_tokens(expired_at);

CREATE UNIQUE INDEX uq_password_reset_tokens_active_user
    ON password_reset_tokens(user_id)
    WHERE used_at IS NULL AND revoked_at IS NULL;

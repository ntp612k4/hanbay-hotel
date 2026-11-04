CREATE TABLE account_access_audits (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL REFERENCES users(id),
    user_id UUID NOT NULL REFERENCES users(id),
    action VARCHAR(30) NOT NULL CHECK (action IN ('CREATE', 'UPDATE_ROLES', 'UPDATE_STATUS')),
    old_roles TEXT,
    new_roles TEXT NOT NULL,
    old_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_account_access_audits_user_created ON account_access_audits (user_id, created_at DESC);

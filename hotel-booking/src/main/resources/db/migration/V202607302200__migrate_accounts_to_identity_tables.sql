ALTER TABLE users
    ADD COLUMN email VARCHAR(255) UNIQUE,
    ADD COLUMN password_hash VARCHAR(255),
    ADD COLUMN full_name VARCHAR(150),
    ADD COLUMN phone VARCHAR(30),
    ADD COLUMN avatar_url TEXT,
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN updated_at TIMESTAMPTZ,
    ADD CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED'));

CREATE UNIQUE INDEX uq_users_phone_not_null ON users (phone) WHERE phone IS NOT NULL;

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expired_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

INSERT INTO roles (code, name)
VALUES
    ('GUEST', 'Guest'),
    ('RECEPTIONIST', 'Receptionist'),
    ('MANAGER', 'Manager');

DO $$
BEGIN
    IF to_regclass('public.accounts') IS NOT NULL THEN
        EXECUTE $migration$
            INSERT INTO users (id, email, password_hash, full_name, phone, status, created_at, updated_at)
            SELECT
                id,
                COALESCE(email, 'legacy-' || id || '@invalid.local'),
                password_hash,
                full_name,
                phone,
                CASE WHEN enabled THEN 'ACTIVE' ELSE 'INACTIVE' END,
                created_at,
                updated_at
            FROM accounts
        $migration$;

        EXECUTE $migration$
            INSERT INTO user_roles (user_id, role_id)
            SELECT accounts.id, roles.id
            FROM accounts
            JOIN roles ON roles.code = CASE accounts.role
                WHEN 'STAFF' THEN 'RECEPTIONIST'
                WHEN 'ADMIN' THEN 'ADMIN'
                ELSE accounts.role
            END
        $migration$;

        DROP TABLE accounts;
    END IF;
END $$;

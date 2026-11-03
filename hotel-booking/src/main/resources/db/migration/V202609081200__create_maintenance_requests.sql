CREATE TYPE maintenance_request_status AS ENUM (
    'PENDING',
    'IN_PROGRESS',
    'COMPLETED'
);

CREATE TABLE maintenance_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id UUID NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    status maintenance_request_status NOT NULL DEFAULT 'PENDING',
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,

    CONSTRAINT fk_maintenance_request_room
        FOREIGN KEY (room_id)
        REFERENCES rooms(id),

    CONSTRAINT fk_maintenance_request_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id),

    CONSTRAINT chk_maintenance_request_timestamps CHECK (
        (status = 'PENDING' AND started_at IS NULL AND completed_at IS NULL)
        OR (status = 'IN_PROGRESS' AND started_at IS NOT NULL AND completed_at IS NULL)
        OR (status = 'COMPLETED' AND started_at IS NOT NULL AND completed_at IS NOT NULL)
    )
);

CREATE INDEX idx_maintenance_requests_created_at
    ON maintenance_requests(created_at DESC);

CREATE UNIQUE INDEX uq_maintenance_requests_active_room
    ON maintenance_requests(room_id)
    WHERE status IN ('PENDING', 'IN_PROGRESS');

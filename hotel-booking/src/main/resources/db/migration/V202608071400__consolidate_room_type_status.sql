UPDATE room_types
SET status = CASE
    WHEN active THEN 'ACTIVE'::room_type_status
    ELSE 'INACTIVE'::room_type_status
END;

DROP INDEX IF EXISTS idx_room_types_active_name;

ALTER TABLE room_types
    DROP COLUMN IF EXISTS active;

CREATE INDEX idx_room_types_status_name ON room_types (status, name);

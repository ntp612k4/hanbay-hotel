ALTER TABLE room_types
    ADD COLUMN capacity_adults INTEGER NOT NULL DEFAULT 1 CHECK (capacity_adults >= 1),
    ADD COLUMN capacity_children INTEGER NOT NULL DEFAULT 0 CHECK (capacity_children >= 0),
    ADD COLUMN thumbnail_url TEXT,
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE room_types
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX idx_room_types_active_name ON room_types (active, name);

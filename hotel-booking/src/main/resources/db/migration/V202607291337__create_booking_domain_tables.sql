CREATE TYPE room_type_status AS ENUM (
    'ACTIVE',
    'INACTIVE'
);

CREATE TABLE room_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    base_price BIGINT NOT NULL CHECK(base_price >= 0),
    max_guests INT NOT NULL CHECK (max_guests > 0),
    bed_type VARCHAR(100),
    area_m2 DECIMAL(6, 2) CHECK(area_m2 IS NULL OR area_m2 > 0),
    status room_type_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE room_type_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_id UUID NOT NULL,
    room_type_id UUID NOT NULL,
    image_url TEXT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0 CHECK(sort_order>=0),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,

    CONSTRAINT fk_room_type_room_type_image
        FOREIGN KEY (room_type_id)
        REFERENCES room_types(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_room_type_id ON room_type_images(room_type_id);

CREATE TYPE room_status AS ENUM (
    'AVAILABLE',
    'OCCUPIED',
    'DIRTY',
    'CLEANING',
    'MAINTENANCE',
    'OUT_OF_SERVICE'
);

CREATE TABLE rooms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_type_id UUID NOT NULL,
    room_number VARCHAR(30) NOT NULL UNIQUE,
    floor INT,
    status room_status NOT NULL DEFAULT 'AVAILABLE',
    note TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,

    CONSTRAINT fk_room_room_type
        FOREIGN KEY(room_type_id)
        REFERENCES room_types(id)
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid()
);

CREATE TYPE booking_status AS ENUM (
    'PENDING',
    'CONFIRMED',
    'CANCELLED',
    'CHECKED_IN',
    'CHECKED_OUT',
    'EXPIRED'
);

CREATE TABLE bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_code VARCHAR(50) UNIQUE NOT NULL,
    booked_by UUID,
    room_type_id UUID NOT NULL,
    assigned_room_id UUID,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL CHECK(check_out_date > check_in_date),
    adult_count INT NOT NULL DEFAULT 1,
    child_count INT NOT NULL DEFAULT 0,
    status booking_status NOT NULL DEFAULT 'PENDING',
    total_amount BIGINT NOT NULL CHECK (total_amount >= 0),
    paid_amount BIGINT NOT NULL DEFAULT 0 CHECK (paid_amount >= 0),
    expired_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,

    CONSTRAINT fk_booking_guest
        FOREIGN KEY (booked_by)
        REFERENCES users(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_booking_room_type
        FOREIGN KEY (room_type_id)
        REFERENCES room_types(id),

    CONSTRAINT fk_booking_assigned_room
        FOREIGN KEY(assigned_room_id)
        REFERENCES rooms(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_booked_by ON bookings(booked_by);
CREATE INDEX idx_room_booking ON bookings(room_type_id, check_in_date, check_out_date);
CREATE INDEX idx_status ON bookings(status);

CREATE TABLE booking_guests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_id UUID NOT NULL,
    booking_id UUID NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(30),
    email VARCHAR(255),
    identity_number VARCHAR(50),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,

    UNIQUE(booking_id, identity_number),

    CONSTRAINT fk_booking_guest_booking
        FOREIGN KEY(booking_id)
        REFERENCES bookings(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_booking_guest_booking_id ON booking_guests(booking_id);

CREATE TYPE price_rule_type AS ENUM (
    'BASE',
    'WEEKEND',
    'HOLIDAY',
    'SEASONAL',
    'PROMOTION'
);

CREATE TYPE price_rule_adjustment_type AS ENUM (
    'FIXED',
    'PERCENTAGE'
);

CREATE TABLE price_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_id UUID NOT NULL,
    room_type_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    rule_type price_rule_type NOT NULL,
    adjustment_type price_rule_adjustment_type NOT NULL,
    adjustment_value BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    priority INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,

    CHECK(start_date <= end_date),

    CONSTRAINT fk_room_type_price_rule
        FOREIGN KEY (room_type_id)
        REFERENCES room_types(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_price_rules_room_availability ON price_rules(room_type_id, start_date, end_date);

CREATE TABLE booking_nights (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_id UUID NOT NULL,
    booking_id UUID NOT NULL,
    stay_date DATE NOT NULL,
    price_per_night BIGINT NOT NULL CHECK (price_per_night >= 0),
    price_rule_id UUID,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,

    CONSTRAINT fk_booking_night_booking
        FOREIGN KEY (booking_id)
        REFERENCES bookings(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_booking_night_price_rule
        FOREIGN KEY (price_rule_id)
        REFERENCES price_rules(id)
        ON DELETE SET NULL
);

CREATE UNIQUE INDEX idx_booking_nights_booking_date ON booking_nights (booking_id, stay_date);

CREATE TABLE booking_status_histories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_id UUID NOT NULL,
    booking_id UUID NOT NULL,
    old_status booking_status,
    new_status booking_status NOT NULL,
    changed_by UUID,
    reason TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,

    CONSTRAINT fk_booking_status_histories_booking
        FOREIGN KEY (booking_id)
        REFERENCES bookings(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_booking_status_histories_changed_by
        FOREIGN KEY (changed_by)
        REFERENCES users(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_booking_status_history_booking_id ON booking_status_histories(booking_id);

CREATE TYPE add_on_service_pricing_type AS ENUM (
    'PER_BOOKING',
    'PER_NIGHT',
    'PER_GUEST'
);

CREATE TABLE add_on_services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    price BIGINT NOT NULL CHECK (price >= 0),
    pricing_type add_on_service_pricing_type NOT NULL DEFAULT 'PER_BOOKING',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE booking_add_ons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL,
    add_on_service_id UUID NOT NULL,
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
    unit_price BIGINT NOT NULL CHECK (unit_price >= 0),
    total_price BIGINT NOT NULL CHECK (total_price >= 0),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,

    UNIQUE(booking_id, add_on_service_id),

    CONSTRAINT fk_booking_booking_add_ons
        FOREIGN KEY (booking_id)
        REFERENCES bookings(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_add_on_service_booking_add_ons
        FOREIGN KEY (add_on_service_id)
        REFERENCES add_on_services(id)
);

CREATE INDEX idx_booking_add_on_booking_id ON booking_add_ons(booking_id);
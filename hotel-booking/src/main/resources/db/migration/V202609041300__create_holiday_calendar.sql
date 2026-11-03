CREATE TABLE holiday_calendar (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_id UUID NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,

    CHECK (start_date <= end_date)
);

CREATE INDEX idx_holiday_calendar_dates ON holiday_calendar(start_date, end_date);

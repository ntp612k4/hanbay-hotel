ALTER TABLE bookings
    ADD COLUMN cancelled_at TIMESTAMP;

CREATE INDEX idx_bookings_cancelled_at
    ON bookings(cancelled_at)
    WHERE cancelled_at IS NOT NULL;

ALTER TABLE payment_refunds
    ALTER COLUMN reason TYPE VARCHAR(500);

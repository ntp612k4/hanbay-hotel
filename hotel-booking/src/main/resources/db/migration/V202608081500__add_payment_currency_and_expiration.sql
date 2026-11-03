ALTER TABLE payments
    ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    ADD COLUMN expires_at TIMESTAMP;

UPDATE payments AS payment
SET expires_at = COALESCE(
    booking.expired_at,
    payment.created_at + INTERVAL '15 minutes'
)
FROM bookings AS booking
WHERE booking.id = payment.booking_id
  AND payment.expires_at IS NULL;

UPDATE payments
SET expires_at = created_at + INTERVAL '15 minutes'
WHERE expires_at IS NULL;

ALTER TABLE payments
    ALTER COLUMN expires_at SET NOT NULL,
    ADD CONSTRAINT chk_payments_currency
        CHECK (currency = UPPER(currency));

CREATE INDEX idx_payments_status_expires_at
    ON payments(status, expires_at);

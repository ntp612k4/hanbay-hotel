CREATE INDEX IF NOT EXISTS idx_booking_nights_stay_date_booking_id
    ON booking_nights(stay_date, booking_id);

CREATE INDEX IF NOT EXISTS idx_bookings_created_at_status
    ON bookings(created_at, status);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_reporting
    ON payment_transactions(status, currency, occurred_at, type);

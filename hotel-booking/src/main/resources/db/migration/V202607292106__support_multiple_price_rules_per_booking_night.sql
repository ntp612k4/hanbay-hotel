ALTER TABLE booking_nights
DROP CONSTRAINT IF EXISTS fk_booking_night_price_rule;

ALTER TABLE booking_nights
DROP COLUMN IF EXISTS price_rule_id;

CREATE TABLE booking_night_price_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_night_id UUID NOT NULL,
    price_rule_id UUID NOT NULL,
    adjustment_amount BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,

    CONSTRAINT fk_booking_night_price_rule_booking_night
        FOREIGN KEY (booking_night_id)
        REFERENCES booking_nights(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_booking_night_price_rule_price_rule
        FOREIGN KEY (price_rule_id)
        REFERENCES price_rules(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_booking_night_rule
        UNIQUE (booking_night_id, price_rule_id)
);

CREATE INDEX idx_booking_night_price_rules_booking_night
ON booking_night_price_rules(booking_night_id);

CREATE INDEX idx_booking_night_price_rules_rule
ON booking_night_price_rules(price_rule_id);
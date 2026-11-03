CREATE TYPE price_rule_adjustment_direction AS ENUM ('INCREASE', 'DISCOUNT');

ALTER TABLE price_rules
    ADD COLUMN adjustment_direction price_rule_adjustment_direction NOT NULL DEFAULT 'INCREASE';

UPDATE price_rules
SET adjustment_direction = 'DISCOUNT'
WHERE rule_type = 'PROMOTION';

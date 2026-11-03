CREATE TYPE refund_method AS ENUM ('BANK_TRANSFER');

ALTER TABLE payment_refunds
    ADD COLUMN refund_method refund_method,
    ADD COLUMN bank_name VARCHAR(100),
    ADD COLUMN account_holder_name VARCHAR(150),
    ADD COLUMN account_number VARCHAR(34);

ALTER TABLE payment_refunds
    ADD CONSTRAINT chk_refund_destination_complete CHECK (
        (refund_method IS NULL AND bank_name IS NULL AND account_holder_name IS NULL AND account_number IS NULL)
        OR
        (refund_method IS NOT NULL AND bank_name IS NOT NULL AND account_holder_name IS NOT NULL AND account_number IS NOT NULL)
    );

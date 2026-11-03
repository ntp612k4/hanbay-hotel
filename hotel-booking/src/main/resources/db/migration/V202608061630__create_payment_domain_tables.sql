CREATE TYPE payment_status AS ENUM (
    'PENDING',
    'SUCCESS',
    'FAILED',
    'CANCELLED',
    'REFUNDED',
    'EXPIRED',
    'PARTIALLY_REFUNDED'
);

CREATE TYPE payment_transaction_type AS ENUM (
    'PAYMENT',
    'REFUND',
    'REVERSAL',
    'ADJUSTMENT'
);

CREATE TYPE payment_transaction_status AS ENUM (
    'PENDING',
    'SUCCESS',
    'FAILED'
);

CREATE TYPE payment_webhook_deduplication_strategy AS ENUM (
    'EVENT_ID',
    'TRANSACTION_ID',
    'BUSINESS_HASH'
);

CREATE TYPE payment_webhook_processing_status AS ENUM (
    'RECEIVED',
    'PROCESSING',
    'PROCESSED',
    'FAILED',
    'IGNORED'
);

CREATE TYPE payment_refund_status AS ENUM (
    'PENDING',
    'PROCESSING',
    'SUCCESS',
    'FAILED',
    'CANCELLED'
);

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL,
    payment_code VARCHAR(80) NOT NULL UNIQUE,
    amount BIGINT NOT NULL CHECK (amount > 0),
    status payment_status NOT NULL DEFAULT 'PENDING',
    idempotency_key VARCHAR(120) UNIQUE,
    paid_at TIMESTAMP,
    failed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,

    CONSTRAINT fk_payments_booking
        FOREIGN KEY (booking_id)
        REFERENCES bookings(id)
);

CREATE INDEX idx_payments_booking_id ON payments(booking_id);
CREATE INDEX idx_payments_status_created_at ON payments(status, created_at);

CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL,
    provider_transaction_id VARCHAR(150) UNIQUE,
    transaction_ref VARCHAR(120),
    type payment_transaction_type NOT NULL,
    status payment_transaction_status NOT NULL,
    amount BIGINT NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    provider_response_code VARCHAR(50),
    provider_message VARCHAR(255),
    raw_payload JSONB,
    occurred_at TIMESTAMP,
    received_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_payment_transactions_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_payment_transactions_currency
        CHECK (currency = UPPER(currency))
);

CREATE INDEX idx_payment_transactions_payment_received_at
    ON payment_transactions(payment_id, received_at);
CREATE INDEX idx_payment_transactions_transaction_ref
    ON payment_transactions(transaction_ref);
CREATE INDEX idx_payment_transactions_status_received_at
    ON payment_transactions(status, received_at);

CREATE TABLE payment_webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID,
    event_id VARCHAR(120) NOT NULL,
    event_type VARCHAR(80),
    deduplication_key VARCHAR(255) NOT NULL UNIQUE,
    deduplication_strategy payment_webhook_deduplication_strategy NOT NULL,
    safe_headers JSONB,
    payload JSONB NOT NULL,
    signature_valid BOOLEAN NOT NULL,
    processing_status payment_webhook_processing_status NOT NULL DEFAULT 'RECEIVED',
    processing_attempts INT NOT NULL DEFAULT 0 CHECK (processing_attempts >= 0),
    processing_started_at TIMESTAMP,
    processed_at TIMESTAMP,
    last_error TEXT,
    provider_response_status_code INT,
    received_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_payment_webhook_events_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_payment_webhook_response_status
        CHECK (provider_response_status_code IS NULL
            OR provider_response_status_code BETWEEN 100 AND 599)
);

CREATE INDEX idx_payment_webhook_events_status_received_at
    ON payment_webhook_events(processing_status, received_at);
CREATE INDEX idx_payment_webhook_events_payment_id
    ON payment_webhook_events(payment_id);
CREATE INDEX idx_payment_webhook_events_event_id
    ON payment_webhook_events(event_id);

CREATE TABLE payment_refunds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL,
    amount DECIMAL(12, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    status payment_refund_status NOT NULL DEFAULT 'PENDING',
    reason VARCHAR(255),
    provider_refund_id VARCHAR(150),
    provider_response_code VARCHAR(50),
    provider_message VARCHAR(255),
    idempotency_key VARCHAR(120) NOT NULL UNIQUE,
    requested_at TIMESTAMP NOT NULL,
    refunded_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_payment_refunds_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_payment_refunds_currency
        CHECK (currency = UPPER(currency))
);

CREATE INDEX idx_payment_refunds_payment_id ON payment_refunds(payment_id);
CREATE INDEX idx_payment_refunds_status_requested_at
    ON payment_refunds(status, requested_at);

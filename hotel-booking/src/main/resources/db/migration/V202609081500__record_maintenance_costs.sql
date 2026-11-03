-- An absent record means unknown cost, never implicitly zero.
CREATE TABLE maintenance_costs (
    request_id UUID PRIMARY KEY REFERENCES maintenance_requests(id),
    amount BIGINT NOT NULL CHECK (amount >= 0 AND amount <= 999999999999),
    expense_date DATE NOT NULL,
    note VARCHAR(2000) NOT NULL,
    revision INTEGER NOT NULL CHECK (revision > 0),
    recorded_by UUID NOT NULL REFERENCES users(id),
    recorded_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_maintenance_costs_expense_date ON maintenance_costs(expense_date);
CREATE TABLE maintenance_cost_history (
    request_id UUID NOT NULL REFERENCES maintenance_requests(id),
    revision INTEGER NOT NULL,
    amount BIGINT NOT NULL,
    expense_date DATE NOT NULL,
    note VARCHAR(2000) NOT NULL,
    recorded_by UUID NOT NULL REFERENCES users(id),
    recorded_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (request_id, revision)
);

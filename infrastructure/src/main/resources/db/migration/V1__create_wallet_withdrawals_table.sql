CREATE SCHEMA IF NOT EXISTS wallet_withdrawals;

CREATE TABLE wallet_withdrawals.wallet_withdrawals (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    fee DECIMAL(19, 4) NOT NULL,
    amount_for_recipient DECIMAL(19, 4) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    failure_reason TEXT,
    wallet_transaction_id_ref VARCHAR(255),
    payment_provider_id_ref VARCHAR(255),
    recipient_first_name VARCHAR(255) NOT NULL,
    recipient_last_name VARCHAR(255) NOT NULL,
    recipient_national_id VARCHAR(255) NOT NULL,
    recipient_account_number VARCHAR(255) NOT NULL,
    recipient_routing_number VARCHAR(255) NOT NULL
);

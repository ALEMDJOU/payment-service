-- Schéma minimal aligné sur liquibase 001 + 002 + 004 (wallets, transactions, webhook outbox).

DROP TABLE IF EXISTS "yy-pay-webhook-outbox" CASCADE;
DROP TABLE IF EXISTS "yy-pay-transactions" CASCADE;
DROP TABLE IF EXISTS "yy-pay-wallets" CASCADE;

CREATE TABLE "yy-pay-wallets" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    balance DECIMAL(19,4) NOT NULL DEFAULT 0.0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, organization_id)
);

CREATE INDEX idx_yy_pay_wallets_user_id ON "yy-pay-wallets"(user_id);
CREATE INDEX idx_yy_pay_wallets_organization_id ON "yy-pay-wallets"(organization_id);

CREATE TABLE "yy-pay-transactions" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id UUID REFERENCES "yy-pay-wallets"(id),
    user_id UUID,
    organization_id UUID NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
    reference VARCHAR(255) NOT NULL UNIQUE,
    fees DECIMAL(19,4) NOT NULL DEFAULT 0.0,
    method VARCHAR(32) NOT NULL,
    stripe_session_id VARCHAR(512),
    callback_url VARCHAR(2048),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_yy_pay_tx_wallet_id ON "yy-pay-transactions"(wallet_id);
CREATE INDEX idx_yy_pay_tx_user_id ON "yy-pay-transactions"(user_id);
CREATE INDEX idx_yy_pay_tx_organization_id ON "yy-pay-transactions"(organization_id);
CREATE INDEX idx_yy_pay_tx_amount ON "yy-pay-transactions"(amount);
CREATE INDEX idx_yy_pay_tx_type ON "yy-pay-transactions"(type);
CREATE INDEX idx_yy_pay_tx_status ON "yy-pay-transactions"(status);
CREATE INDEX idx_yy_pay_tx_reference ON "yy-pay-transactions"(reference);
CREATE INDEX idx_yy_pay_tx_method ON "yy-pay-transactions"(method);

CREATE TABLE "yy-pay-webhook-outbox" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL REFERENCES "yy-pay-transactions"(id),
    event_type VARCHAR(64) NOT NULL,
    callback_url VARCHAR(2048) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_webhook_outbox_status_next ON "yy-pay-webhook-outbox"(status, next_attempt_at);
CREATE INDEX idx_webhook_outbox_transaction_id ON "yy-pay-webhook-outbox"(transaction_id);

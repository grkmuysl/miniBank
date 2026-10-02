CREATE TABLE transfers(
    id BIGSERIAL PRIMARY KEY,
    initiator_id VARCHAR(255) NOT NULL,
    source_iban VARCHAR(26) NOT NULL,
    target_iban VARCHAR(26) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    description VARCHAR(140),
    status VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(100),
    idempotency_key VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL ,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uk_transfers_initiator_idempotency UNIQUE (initiator_id, idempotency_key),
    CONSTRAINT chk_transfers_different_ibans      CHECK (source_iban <> target_iban),
    CONSTRAINT chk_accounts_amount       CHECK (amount >= 0),
    CONSTRAINT chk_accounts_status        CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED'))
);
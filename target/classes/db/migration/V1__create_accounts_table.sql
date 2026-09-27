CREATE TABLE accounts(
    id BIGSERIAL PRIMARY KEY ,
    account_number VARCHAR(26) UNIQUE  NOT NULL,
    owner_name VARCHAR(100) NOT NULL ,
    currency VARCHAR(3) NOT NULL ,
    balance NUMERIC(19,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL ,
    updated_at TIMESTAMPTZ NOT NULL ,

    CONSTRAINT uq_accounts_account_number UNIQUE (account_number),
    CONSTRAINT chk_accounts_balance       CHECK (balance >= 0),
    CONSTRAINT chk_accounts_status        CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),
    CONSTRAINT chk_accounts_currency      CHECK (currency ~ '^[A-Z]{3}$')
);
-- Available balance = balance - held_amount. Held money stays in the account but cannot be spent.
ALTER TABLE accounts
    ADD COLUMN held_amount NUMERIC(19, 2) NOT NULL DEFAULT 0;

ALTER TABLE accounts ADD CONSTRAINT chk_accounts_held_non_negative
    CHECK (held_amount >= 0);

-- Last line of defense: no bug in the application can ever hold more than the account has.
ALTER TABLE accounts ADD CONSTRAINT chk_accounts_held_within_balance
    CHECK (held_amount <= balance);

CREATE TABLE holds (
                       id                BIGSERIAL      PRIMARY KEY,
                       transfer_id       BIGINT         NOT NULL,
                       account_id        BIGINT         NOT NULL,
                       target_account_id BIGINT         NOT NULL,
                       amount            NUMERIC(19, 2) NOT NULL,
                       status            VARCHAR(10)    NOT NULL,
                       version           BIGINT         NOT NULL DEFAULT 0,
                       created_at        TIMESTAMPTZ    NOT NULL,
                       updated_at        TIMESTAMPTZ    NOT NULL,

                       CONSTRAINT uk_holds_transfer        UNIQUE (transfer_id),
                       CONSTRAINT fk_holds_account         FOREIGN KEY (account_id)        REFERENCES accounts (id),
                       CONSTRAINT fk_holds_target_account  FOREIGN KEY (target_account_id) REFERENCES accounts (id),
                       CONSTRAINT chk_holds_amount         CHECK (amount > 0),
                       CONSTRAINT chk_holds_status         CHECK (status IN ('HELD', 'CAPTURED', 'RELEASED')),
                       CONSTRAINT chk_holds_different_accounts CHECK (account_id <> target_account_id)
);

CREATE INDEX idx_holds_account_id ON holds (account_id);
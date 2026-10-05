CREATE TABLE postings (
                          id            BIGSERIAL      PRIMARY KEY,
                          transfer_id   BIGINT         NOT NULL,
                          account_id    BIGINT         NOT NULL,
                          direction     VARCHAR(6)     NOT NULL,
                          amount        NUMERIC(19, 2) NOT NULL,
                          balance_after NUMERIC(19, 2) NOT NULL,
                          created_at    TIMESTAMPTZ    NOT NULL,

                          CONSTRAINT fk_postings_account            FOREIGN KEY (account_id) REFERENCES accounts (id),
                          CONSTRAINT uk_postings_transfer_direction UNIQUE (transfer_id, direction),
                          CONSTRAINT chk_postings_direction         CHECK (direction IN ('DEBIT', 'CREDIT')),
                          CONSTRAINT chk_postings_amount            CHECK (amount > 0)
);

CREATE INDEX idx_postings_account_id ON postings (account_id);
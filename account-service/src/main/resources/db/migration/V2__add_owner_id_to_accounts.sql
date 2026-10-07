ALTER TABLE accounts
    ADD COLUMN owner_id VARCHAR(255) NOT NULL;

CREATE INDEX idx_accounts_owner_id ON accounts (owner_id);
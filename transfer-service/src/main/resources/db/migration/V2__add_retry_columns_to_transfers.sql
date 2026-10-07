ALTER TABLE transfers
    ADD COLUMN attempt_count   INT          NOT NULL DEFAULT 0,
    ADD COLUMN next_attempt_at TIMESTAMPTZ,
    ADD COLUMN last_error      VARCHAR(255);

-- V1 created these with names copied from the accounts table, and the amount
-- check allowed zero. Replace both with correctly named, correct constraints.
ALTER TABLE transfers DROP CONSTRAINT chk_accounts_status;
ALTER TABLE transfers ADD CONSTRAINT chk_transfers_status
    CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'MANUAL_REVIEW'));

ALTER TABLE transfers DROP CONSTRAINT chk_accounts_amount;
ALTER TABLE transfers ADD CONSTRAINT chk_transfers_amount
    CHECK (amount > 0);

-- Existing PENDING transfers (from 3.2/3.3 tests) have no next_attempt_at.
-- Without this they would never match the job's query and stay PENDING forever.
UPDATE transfers SET next_attempt_at = now() WHERE status = 'PENDING';

-- The retry job polls only PENDING rows; a partial index stays small
-- even when millions of COMPLETED transfers accumulate.
CREATE INDEX idx_transfers_pending_next_attempt
    ON transfers (next_attempt_at)
    WHERE status = 'PENDING';
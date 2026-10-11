ALTER TABLE transfers DROP CONSTRAINT chk_transfers_status;
ALTER TABLE transfers ADD CONSTRAINT chk_transfers_status
    CHECK (status IN ('PENDING', 'FUNDS_HELD', 'FRAUD_APPROVED', 'FRAUD_REJECTED',
                      'COMPLETED', 'FAILED', 'MANUAL_REVIEW'));
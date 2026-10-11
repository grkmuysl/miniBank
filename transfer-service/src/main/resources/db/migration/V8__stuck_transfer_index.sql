-- PENDING joins CAPTURING and RELEASING in the stuck-transfer warning:
-- a HOLD_FUNDS that ended up in the DLT leaves its transfer PENDING forever.
DROP INDEX idx_transfers_in_flight;
CREATE INDEX idx_transfers_in_flight ON transfers (updated_at)
    WHERE status IN ('PENDING', 'CAPTURING', 'RELEASING');
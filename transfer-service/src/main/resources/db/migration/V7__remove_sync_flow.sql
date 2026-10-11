-- Synchronous transfers  that never reached a result: with the retry job gone, nothing would
-- ever drive them again. Their outcome is unknown, so they go to an operator, not to FAILED.
UPDATE transfers
SET status = 'MANUAL_REVIEW', failure_reason = 'LEGACY_SYNC_FLOW_UNRESOLVED'
WHERE status = 'PENDING' AND next_attempt_at IS NOT NULL;

-- Retry bookkeeping of the synchronous flow. The partial index on next_attempt_at is dropped
-- together with the column.
ALTER TABLE transfers
DROP COLUMN attempt_count,
    DROP COLUMN next_attempt_at,
    DROP COLUMN last_error;
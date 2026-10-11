ALTER TABLE transfers DROP CONSTRAINT chk_transfers_status;

ALTER TABLE transfers
    ADD COLUMN release_reason    VARCHAR(30),
    ADD COLUMN state_deadline_at TIMESTAMPTZ;

-- 5.3 left transfers in FRAUD_APPROVED / FRAUD_REJECTED with money still held.
-- Map them to the new model AND queue the command that moves them on; changing
-- only the status would leave them waiting for a command nobody ever sends.
-- gen_random_uuid() is evaluated per row in the CTE, so every message gets its own ID.
WITH pending_commands AS (
    SELECT id, request_id, gen_random_uuid() AS message_id,
           CASE status WHEN 'FRAUD_APPROVED' THEN 'CAPTURE_FUNDS' ELSE 'RELEASE_FUNDS' END AS type
    FROM transfers
    WHERE status IN ('FRAUD_APPROVED', 'FRAUD_REJECTED')
)
INSERT INTO outbox_events (message_id, topic, message_key, payload, status, created_at)
SELECT message_id,
       'account.commands',
       id::text,
    json_build_object(
            'messageId',  message_id,
            'type',       type,
            'transferId', id,
            'requestId',  request_id,
            'occurredAt', to_char(now() AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"'),
            'payload',    '{}'::json)::text,
    'PENDING',
       now()
FROM pending_commands;

UPDATE transfers SET status = 'CAPTURING'
WHERE status = 'FRAUD_APPROVED';

UPDATE transfers SET status = 'RELEASING', release_reason = 'FRAUD_REJECTED'
WHERE status = 'FRAUD_REJECTED';

-- Transfers waiting for a fraud decision get a deadline; if none arrives they time out and
-- are released. (Transfers held in 5.2, before fraud-service existed, will never get one.)
UPDATE transfers SET state_deadline_at = now() + interval '5 minutes'
WHERE status = 'FUNDS_HELD';

-- MANUAL_REVIEW stays as a legacy state of the synchronous flow (3.4); see part 3.
ALTER TABLE transfers ADD CONSTRAINT chk_transfers_status
    CHECK (status IN ('PENDING', 'FUNDS_HELD', 'CAPTURING', 'RELEASING',
                      'COMPLETED', 'FAILED', 'REJECTED', 'MANUAL_REVIEW'));

ALTER TABLE transfers ADD CONSTRAINT chk_transfers_release_reason
    CHECK (release_reason IS NULL OR release_reason IN ('FRAUD_REJECTED', 'CAPTURE_FAILED', 'FRAUD_TIMEOUT'));

-- Timeout job: FUNDS_HELD transfers past their deadline.
CREATE INDEX idx_transfers_fraud_deadline ON transfers (state_deadline_at) WHERE status = 'FUNDS_HELD';

-- Stuck-transfer warning: CAPTURING / RELEASING for too long.
CREATE INDEX idx_transfers_in_flight ON transfers (updated_at) WHERE status IN ('CAPTURING', 'RELEASING');
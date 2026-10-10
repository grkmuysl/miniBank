-- New saga state: money is reserved on the source account, waiting for the next step.
ALTER TABLE transfers DROP CONSTRAINT chk_transfers_status;
ALTER TABLE transfers ADD CONSTRAINT chk_transfers_status
    CHECK (status IN ('PENDING', 'FUNDS_HELD', 'COMPLETED', 'FAILED', 'MANUAL_REVIEW'));

CREATE TABLE outbox_events (
                               id           BIGSERIAL    PRIMARY KEY,
                               message_id   UUID         NOT NULL,
                               topic        VARCHAR(100) NOT NULL,
                               message_key  VARCHAR(100) NOT NULL,
                               payload      TEXT         NOT NULL,
                               status       VARCHAR(10)  NOT NULL,
                               created_at   TIMESTAMPTZ  NOT NULL,
                               published_at TIMESTAMPTZ,

                               CONSTRAINT uk_outbox_message_id UNIQUE (message_id),
                               CONSTRAINT chk_outbox_status    CHECK (status IN ('PENDING', 'PUBLISHED'))
);

CREATE INDEX idx_outbox_pending ON outbox_events (id) WHERE status = 'PENDING';

CREATE TABLE processed_messages (
                                    message_id   UUID        PRIMARY KEY,
                                    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
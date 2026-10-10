-- Outbox: messages written in the same transaction as the business change,
-- published to Kafka afterwards by a poller.
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

-- The poller only reads PENDING rows, in id order.
CREATE INDEX idx_outbox_pending ON outbox_events (id) WHERE status = 'PENDING';

-- Inbox: IDs of messages already processed. A redelivered message is recognised and skipped.
CREATE TABLE processed_messages (
                                    message_id   UUID        PRIMARY KEY,
                                    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
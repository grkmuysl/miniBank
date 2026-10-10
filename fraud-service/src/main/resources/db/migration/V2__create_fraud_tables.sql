-- Every decision, with its reasons. Doubles as the audit trail:
-- "why was this transfer blocked?" must always be answerable. Append-only, never updated.
CREATE TABLE fraud_checks (
                              id           BIGSERIAL      PRIMARY KEY,
                              transfer_id  BIGINT         NOT NULL,
                              initiator_id VARCHAR(255)   NOT NULL,
                              source_iban  VARCHAR(26)    NOT NULL,
                              target_iban  VARCHAR(26)    NOT NULL,
                              amount       NUMERIC(19, 2) NOT NULL,
                              currency     VARCHAR(3)     NOT NULL,
                              decision     VARCHAR(10)    NOT NULL,
                              reasons      VARCHAR(500),
                              requested_at TIMESTAMPTZ    NOT NULL,   -- when the transfer was created (business time)
                              created_at   TIMESTAMPTZ    NOT NULL,   -- when we decided (processing time)

                              CONSTRAINT uk_fraud_checks_transfer  UNIQUE (transfer_id),
                              CONSTRAINT chk_fraud_checks_decision CHECK (decision IN ('APPROVED', 'REJECTED')),
    -- A rejection without reasons cannot be explained; an approval has none.
                              CONSTRAINT chk_fraud_checks_reasons  CHECK (
                                  (decision = 'APPROVED' AND reasons IS NULL) OR
                                  (decision = 'REJECTED' AND reasons IS NOT NULL))
);

-- Velocity rule: checks of one customer in a time window.
CREATE INDEX idx_fraud_checks_initiator_time ON fraud_checks (initiator_id, requested_at);

-- New-beneficiary rule: has this customer ever been approved for this target?
CREATE INDEX idx_fraud_checks_approved_beneficiary
    ON fraud_checks (initiator_id, target_iban)
    WHERE decision = 'APPROVED';

CREATE TABLE blocked_ibans (
                               iban       VARCHAR(26)  PRIMARY KEY,
                               reason     VARCHAR(255) NOT NULL,
                               created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
package com.gorkemuysal.minibank.transfer.client;

/**
 * What we know after calling account-service.
 * The three cases map directly to transfer statuses: COMPLETED, FAILED, PENDING.
 */
public sealed interface PostingOutcome {

    /** Money moved. */
    record Posted() implements PostingOutcome {
    }

    /** account-service rejected the request for a business reason. Money did NOT move. */
    record Rejected(String errorCode) implements PostingOutcome {
    }

    /** We don't know whether money moved (timeout, connection error, 5xx...). */
    record Unknown(String reason) implements PostingOutcome {
    }
}
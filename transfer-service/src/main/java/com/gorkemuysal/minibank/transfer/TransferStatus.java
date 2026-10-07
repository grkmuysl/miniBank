package com.gorkemuysal.minibank.transfer;

public enum TransferStatus {
    PENDING,
    COMPLETED,
    FAILED,
    /** Outcome still unknown after all retries. An operator must check account-service manually. */
    MANUAL_REVIEW
}
package com.gorkemuysal.minibank.transfer;

public enum TransferStatus {
    PENDING,
    /** Saga: money is reserved on the source account. */
    FUNDS_HELD,
    /** Saga: fraud check passed; money still held, waiting to be captured (5.4). */
    FRAUD_APPROVED,
    /** Saga: fraud check failed; money still held, waiting to be released (5.4). */
    FRAUD_REJECTED,
    COMPLETED,
    FAILED,
    MANUAL_REVIEW
}
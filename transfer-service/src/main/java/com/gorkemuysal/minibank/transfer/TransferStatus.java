package com.gorkemuysal.minibank.transfer;

public enum TransferStatus {
    PENDING,
    /** Saga: money is reserved on the source account; waiting for the next step. */
    FUNDS_HELD,
    COMPLETED,
    FAILED,
    MANUAL_REVIEW
}
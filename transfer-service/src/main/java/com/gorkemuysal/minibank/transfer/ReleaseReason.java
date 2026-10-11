package com.gorkemuysal.minibank.transfer;

/** Why a transfer is being (or was) compensated. Stored for customers and operations. */
public enum ReleaseReason {
    FRAUD_REJECTED,
    CAPTURE_FAILED,
    FRAUD_TIMEOUT
}
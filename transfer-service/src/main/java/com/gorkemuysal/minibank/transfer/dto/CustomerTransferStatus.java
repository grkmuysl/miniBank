package com.gorkemuysal.minibank.transfer.dto;

import com.gorkemuysal.minibank.transfer.TransferStatus;

/**
 * What customers see. Saga-internal states are folded into PROCESSING, so adding a saga step
 * later (e.g. a second approval in step 7) doesn't change the public API.
 */
public enum CustomerTransferStatus {
    PROCESSING,
    COMPLETED,
    REJECTED,
    FAILED;

    public static CustomerTransferStatus of(TransferStatus internal) {
        return switch (internal) {
            case PENDING, FUNDS_HELD, CAPTURING, RELEASING -> PROCESSING;
            // Outcome of a legacy synchronous transfer is unknown until an operator resolves it.
            case MANUAL_REVIEW -> PROCESSING;
            case COMPLETED -> COMPLETED;
            case REJECTED -> REJECTED;
            case FAILED -> FAILED;
        };
    }
}
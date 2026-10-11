package com.gorkemuysal.minibank.transfer;

import java.util.EnumSet;
import java.util.Set;

/**
 * Internal saga states. Each state names what the saga is doing right now.
 * Not shown to customers as-is (see CustomerTransferStatus).
 */
public enum TransferStatus {
    PENDING,        // hold requested
    FUNDS_HELD,     // money held, waiting for the fraud decision
    CAPTURING,      // fraud approved, capture requested (pivot: no way back after this succeeds)
    RELEASING,      // compensation running, release requested
    COMPLETED,      // final: money moved to the target
    FAILED,         // final: hold could not be placed, nothing to compensate
    REJECTED,       // final: hold released, money back with the customer
    MANUAL_REVIEW;  // final: legacy state of the synchronous flow (3.4)

    /** The saga's transition table. Anything not listed here is refused. */
    public Set<TransferStatus> allowedNext() {
        return switch (this) {
            case PENDING -> EnumSet.of(FUNDS_HELD, FAILED);
            case FUNDS_HELD -> EnumSet.of(CAPTURING, RELEASING);
            case CAPTURING -> EnumSet.of(COMPLETED, RELEASING);
            case RELEASING -> EnumSet.of(REJECTED);
            case COMPLETED, FAILED, REJECTED, MANUAL_REVIEW -> EnumSet.noneOf(TransferStatus.class);
        };
    }

    public boolean canTransitionTo(TransferStatus next) {
        return allowedNext().contains(next);
    }

    public boolean isFinal() {
        return allowedNext().isEmpty();
    }
}
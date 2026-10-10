package com.gorkemuysal.minibank.hold;

/**
 * Outcome of placing a hold, for the messaging path.
 * Business failures are values here, not exceptions (see HoldService.tryPlaceHold).
 */
public sealed interface HoldResult {

    record Held(Long holdId, java.math.BigDecimal amount) implements HoldResult {
    }

    record Rejected(String code, String message) implements HoldResult {
    }
}
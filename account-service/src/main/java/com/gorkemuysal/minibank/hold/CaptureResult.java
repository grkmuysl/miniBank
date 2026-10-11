package com.gorkemuysal.minibank.hold;

import java.math.BigDecimal;

/** Outcome of capturing a hold, for the messaging path (see HoldService.tryCapture). */
public sealed interface CaptureResult {

    record Captured(BigDecimal amount) implements CaptureResult {
    }

    record Rejected(String code, String message) implements CaptureResult {
    }
}
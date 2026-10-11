package com.gorkemuysal.minibank.fraud.messaging.payload;

import java.math.BigDecimal;
import java.time.Instant;

/** requestedAt: when the transfer was created. Rules use it as "now" (see VelocityRule). */
public record FraudCheckRequestedPayload(
        String initiatorId,
        String sourceIban,
        String targetIban,
        BigDecimal amount,
        String currency,
        Instant requestedAt
) {
}
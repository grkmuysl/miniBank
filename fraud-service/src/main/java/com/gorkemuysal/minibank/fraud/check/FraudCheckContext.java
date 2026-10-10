package com.gorkemuysal.minibank.fraud.check;

import java.math.BigDecimal;
import java.time.Instant;

/** Everything the rules may look at for one transfer. */
public record FraudCheckContext(
        Long transferId,
        String initiatorId,
        String sourceIban,
        String targetIban,
        BigDecimal amount,
        String currency,
        Instant requestedAt
) {
}
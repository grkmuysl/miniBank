package com.gorkemuysal.minibank.transfer.messaging.payload;

import com.gorkemuysal.minibank.transfer.Currency;
import com.gorkemuysal.minibank.transfer.Transfer;

import java.math.BigDecimal;
import java.time.Instant;

/** requestedAt = when the transfer was created; fraud rules use it as their reference time. */
public record FraudCheckRequestedPayload(
        String initiatorId,
        String sourceIban,
        String targetIban,
        BigDecimal amount,
        Currency currency,
        Instant requestedAt
) {
    public static FraudCheckRequestedPayload from(Transfer t) {
        return new FraudCheckRequestedPayload(t.getInitiatorId(), t.getSourceIban(),
                t.getTargetIban(), t.getAmount(), t.getCurrency(), t.getCreatedAt());
    }
}
package com.gorkemuysal.minibank.messaging.payload;

import com.gorkemuysal.minibank.account.Currency;

import java.math.BigDecimal;

public record HoldFundsPayload(
        String initiatorId,
        String sourceIban,
        String targetIban,
        BigDecimal amount,
        Currency currency
) {
}
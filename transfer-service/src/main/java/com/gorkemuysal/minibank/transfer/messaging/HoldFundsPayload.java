package com.gorkemuysal.minibank.transfer.messaging;


import com.gorkemuysal.minibank.transfer.Currency;

import java.math.BigDecimal;

public record HoldFundsPayload(
        String initiatorId,
        String sourceIban,
        String targetIban,
        BigDecimal amount,
        Currency currency
) {
}
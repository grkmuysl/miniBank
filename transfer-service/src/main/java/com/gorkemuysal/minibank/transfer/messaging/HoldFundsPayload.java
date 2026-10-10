package com.gorkemuysal.minibank.transfer.messaging;


import com.gorkemuysal.minibank.transfer.Currency;
import com.gorkemuysal.minibank.transfer.Transfer;

import java.math.BigDecimal;

public record HoldFundsPayload(
        String initiatorId,
        String sourceIban,
        String targetIban,
        BigDecimal amount,
        Currency currency
) {
    public static HoldFundsPayload from(Transfer transfer) {
        return new HoldFundsPayload(transfer.getInitiatorId(), transfer.getSourceIban(),
                transfer.getTargetIban(), transfer.getAmount(), transfer.getCurrency());
    }
}
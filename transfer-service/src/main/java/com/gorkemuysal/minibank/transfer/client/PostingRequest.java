package com.gorkemuysal.minibank.transfer.client;

import com.gorkemuysal.minibank.transfer.Currency;

import java.math.BigDecimal;

/** Request body of account-service's POST /internal/v1/postings. */
public record PostingRequest(
        Long transferId,
        String initiatorId,
        String sourceIban,
        String targetIban,
        BigDecimal amount,
        Currency currency
) {
}
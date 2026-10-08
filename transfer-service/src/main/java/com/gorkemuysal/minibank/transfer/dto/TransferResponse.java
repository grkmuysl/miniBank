package com.gorkemuysal.minibank.transfer.dto;

import com.gorkemuysal.minibank.transfer.Currency;
import com.gorkemuysal.minibank.transfer.Transfer;
import com.gorkemuysal.minibank.transfer.TransferStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponse(
        Long id,
        String sourceIban,
        String targetIban,
        BigDecimal amount,
        Currency currency,
        String description,
        TransferStatus status,
        String failureReason,
        String requestId,     // reference number: ID of the request that created this transfer
        Instant createdAt
) {
    public static TransferResponse from(Transfer t) {
        return new TransferResponse(
                t.getId(), t.getSourceIban(), t.getTargetIban(), t.getAmount(),
                t.getCurrency(), t.getDescription(), t.getStatus(),
                t.getFailureReason(), t.getRequestId(), t.getCreatedAt());
    }
}
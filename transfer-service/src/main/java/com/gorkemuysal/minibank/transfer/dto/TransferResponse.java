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
    /** Generic code shown to customers instead of the exact fraud rules (see 5.3). */
    static final String RISK_DECLINED = "DECLINED_BY_RISK_CHECK";

    public static TransferResponse from(Transfer t) {
        // Exact rule codes would teach an attacker how to evade them; keep them internal.
        String failureReason = t.getStatus() == TransferStatus.FRAUD_REJECTED
                ? RISK_DECLINED
                : t.getFailureReason();

        return new TransferResponse(
                t.getId(), t.getSourceIban(), t.getTargetIban(), t.getAmount(),
                t.getCurrency(), t.getDescription(), t.getStatus(),
                failureReason, t.getRequestId(), t.getCreatedAt());
    }
}
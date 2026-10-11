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
        CustomerTransferStatus status,
        String failureReason,
        String requestId,     // reference number: ID of the request that created this transfer
        Instant createdAt
) {

    public static TransferResponse from(Transfer t) {
        return new TransferResponse(
                t.getId(), t.getSourceIban(), t.getTargetIban(), t.getAmount(),
                t.getCurrency(), t.getDescription(),
                CustomerTransferStatus.of(t.getStatus()),
                customerReason(t),
                t.getRequestId(), t.getCreatedAt());
    }

    /**
     * Never expose internal details: exact fraud rules would teach evasion, and a capture
     * failure like ACCOUNT_NOT_ACTIVE would reveal the state of someone else's account.
     */
    private static String customerReason(Transfer t) {
        return switch (t.getStatus()) {
            case FAILED -> t.getFailureReason();   // about the customer's own account
            case REJECTED -> switch (t.getReleaseReason()) {
                case FRAUD_REJECTED -> "DECLINED_BY_RISK_CHECK";
                case FRAUD_TIMEOUT -> "RISK_CHECK_TIMED_OUT";
                case CAPTURE_FAILED -> "TRANSFER_COULD_NOT_BE_COMPLETED";
            };
            default -> null;
        };
    }
}
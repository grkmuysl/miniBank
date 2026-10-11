package com.gorkemuysal.minibank.transfer;

import com.gorkemuysal.minibank.transfer.common.BaseEntity;
import com.gorkemuysal.minibank.transfer.dto.CreateTransferRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "transfers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transfer extends BaseEntity {

    private static final Duration INITIAL_RETRY_DELAY = Duration.ofSeconds(30);

    @Column(nullable = false, length = 64)
    private String idempotencyKey;

    @Column(nullable = false)
    private String initiatorId;

    @Column(nullable = false, length = 26)
    private String sourceIban;

    @Column(nullable = false, length = 26)
    private String targetIban;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Column(length = 140)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferStatus status;

    @Column(length = 100)
    private String failureReason;

    @Version
    private Long version;

    @Column(nullable = false)
    private int attemptCount;

    private Instant nextAttemptAt;

    @Column(length = 255)
    private String lastError;

    @Column(length = 64, updatable = false)
    private String requestId;

    /** Synchronous flow (3.x): first attempt in the request, retries by the job. */
    public static Transfer createPending(String initiatorId, String idempotencyKey,
                                         CreateTransferRequest request, String requestId) {
        Transfer transfer = newPending(initiatorId, idempotencyKey, request, requestId);
        transfer.nextAttemptAt = Instant.now().plus(INITIAL_RETRY_DELAY);
        return transfer;
    }

    /**
     * Saga flow (5.x): driven by Kafka messages. nextAttemptAt stays NULL on purpose,
     * so the synchronous retry job never picks this transfer up and moves money over HTTP.
     */
    public static Transfer createForSaga(String initiatorId, String idempotencyKey,
                                         CreateTransferRequest request, String requestId) {
        return newPending(initiatorId, idempotencyKey, request, requestId);
    }

    private static Transfer newPending(String initiatorId, String idempotencyKey,
                                       CreateTransferRequest request, String requestId) {
        Transfer transfer = new Transfer();
        transfer.initiatorId = initiatorId;
        transfer.idempotencyKey = idempotencyKey;
        transfer.sourceIban = request.sourceIban();
        transfer.targetIban = request.targetIban();
        transfer.amount = request.amount();
        transfer.currency = request.currency();
        transfer.description = request.description();
        transfer.requestId = requestId;
        transfer.status = TransferStatus.PENDING;
        return transfer;
    }

    public boolean matches(CreateTransferRequest request) {
        return sourceIban.equals(request.sourceIban())
                && targetIban.equals(request.targetIban())
                && amount.compareTo(request.amount()) == 0
                && currency == request.currency();
    }

    public boolean isPending() {
        return status == TransferStatus.PENDING;
    }

    public void markCompleted() {
        requirePending();
        this.status = TransferStatus.COMPLETED;
        this.nextAttemptAt = null;
        this.lastError = null;
    }

    public void markFailed(String reason) {
        requirePending();
        this.status = TransferStatus.FAILED;
        this.failureReason = truncate(reason, 100);
        this.nextAttemptAt = null;
    }

    /** Saga: account-service confirmed the hold. */
    public void markFundsHeld() {
        requirePending();
        this.status = TransferStatus.FUNDS_HELD;
    }

    /** reasons: all triggered rule codes, stored for support and audit (not shown to the customer). */
    public void markFraudRejected(String reasons) {
        requireStatus(TransferStatus.FUNDS_HELD);
        this.status = TransferStatus.FRAUD_REJECTED;
        this.failureReason = truncate(reasons, 100);
    }

    public void markFraudApproved() {
        requireStatus(TransferStatus.FUNDS_HELD);
        this.status = TransferStatus.FRAUD_APPROVED;
    }

    /** Outcome unknown; try again at the given time. */
    public void scheduleRetry(String error, Instant nextAttemptAt) {
        requirePending();
        this.attemptCount++;
        this.lastError = truncate(error, 255);
        this.nextAttemptAt = nextAttemptAt;
    }

    /** Outcome still unknown after the last allowed attempt; hand over to a human. */
    public void markManualReview(String error) {
        requirePending();
        this.attemptCount++;
        this.status = TransferStatus.MANUAL_REVIEW;
        this.lastError = truncate(error, 255);
        this.nextAttemptAt = null;
    }

    /** Final states never change: a COMPLETED transfer must not silently become FAILED. */
    private void requirePending() {
        requireStatus(TransferStatus.PENDING);
    }

    /** Each saga step may only start from the state that expects it. */
    private void requireStatus(TransferStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(
                    "Transfer " + getId() + " must be " + expected + " but is " + status);
        }
    }

    private static String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
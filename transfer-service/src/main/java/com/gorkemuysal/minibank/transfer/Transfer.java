package com.gorkemuysal.minibank.transfer;

import com.gorkemuysal.minibank.transfer.common.BaseEntity;
import com.gorkemuysal.minibank.transfer.dto.CreateTransferRequest;
import com.gorkemuysal.minibank.transfer.exception.SagaInvariantViolationException;
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

    @Column(length = 64, updatable = false)
    private String requestId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ReleaseReason releaseReason;

    /** Only set while FUNDS_HELD: when the fraud check times out. */
    private Instant stateDeadlineAt;

    /**
     * Saga flow (5.x): driven by Kafka messages. nextAttemptAt stays NULL on purpose,
     * so the synchronous retry job never picks this transfer up and moves money over HTTP.
     */
    public static Transfer create(String initiatorId, String idempotencyKey,
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


    // ---- Saga events. Each one accepts exactly one current state. ----

    public void onFundsHeld(Instant fraudDeadline) {
        requireStatus(TransferStatus.PENDING);
        transitionTo(TransferStatus.FUNDS_HELD);
        this.stateDeadlineAt = fraudDeadline;
    }

    public void onHoldFailed(String code) {
        requireStatus(TransferStatus.PENDING);
        transitionTo(TransferStatus.FAILED);
        this.failureReason = truncate(code, 100);
    }

    public void onFraudApproved() {
        requireStatus(TransferStatus.FUNDS_HELD);
        transitionTo(TransferStatus.CAPTURING);
        this.stateDeadlineAt = null;
    }

    public void onFraudRejected(String reasons) {
        requireStatus(TransferStatus.FUNDS_HELD);
        startReleasing(ReleaseReason.FRAUD_REJECTED);
        this.failureReason = truncate(reasons, 100);   // rule codes, internal only
    }

    public void onFraudTimeout() {
        requireStatus(TransferStatus.FUNDS_HELD);
        startReleasing(ReleaseReason.FRAUD_TIMEOUT);
    }

    public void onCaptured() {
        requireStatus(TransferStatus.CAPTURING);
        transitionTo(TransferStatus.COMPLETED);
    }

    public void onCaptureFailed(String code) {
        requireStatus(TransferStatus.CAPTURING);
        startReleasing(ReleaseReason.CAPTURE_FAILED);
        this.failureReason = truncate(code, 100);
    }

    public void onReleased() {
        requireStatus(TransferStatus.RELEASING);
        transitionTo(TransferStatus.REJECTED);
    }

    private void startReleasing(ReleaseReason reason) {
        transitionTo(TransferStatus.RELEASING);
        this.releaseReason = reason;
        this.stateDeadlineAt = null;
    }

    /** Enforces the transition table; a refused transition is a bug, not a business outcome. */
    private void transitionTo(TransferStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new SagaInvariantViolationException(
                    "Transfer " + getId() + ": transition " + status + " -> " + next + " is not allowed");
        }
        this.status = next;
    }

    /** Each saga step may only start from the state that expects it. */
    private void requireStatus(TransferStatus expected) {
        if (status != expected) {
            throw new SagaInvariantViolationException(
                    "Transfer " + getId() + " must be " + expected + " but is " + status);
        }
    }

    private static String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
package com.gorkemuysal.minibank.transfer;

import com.gorkemuysal.minibank.transfer.common.BaseEntity;
import com.gorkemuysal.minibank.transfer.dto.CreateTransferRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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

    public static Transfer createPending(String initiatorId, String idempotencyKey, CreateTransferRequest request) {
        Transfer transfer = new Transfer();
        transfer.initiatorId = initiatorId;
        transfer.idempotencyKey = idempotencyKey;
        transfer.sourceIban = request.sourceIban();
        transfer.targetIban = request.targetIban();
        transfer.amount = request.amount();
        transfer.currency = request.currency();
        transfer.description = request.description();
        transfer.status = TransferStatus.PENDING;
        return transfer;
    }

    public boolean matches(CreateTransferRequest request) {
        return sourceIban.equals(request.sourceIban())
                && targetIban.equals(request.targetIban())
                && amount.compareTo(request.amount()) == 0
                && currency == request.currency();
    }
}
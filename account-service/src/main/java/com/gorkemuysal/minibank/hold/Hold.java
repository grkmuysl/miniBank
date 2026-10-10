package com.gorkemuysal.minibank.hold;

import com.gorkemuysal.minibank.account.Account;
import com.gorkemuysal.minibank.common.BaseEntity;
import com.gorkemuysal.minibank.exception.InvalidHoldStateException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * A reservation of money for one transfer. Lifecycle: HELD -> CAPTURED or HELD -> RELEASED.
 * CAPTURED and RELEASED are final; a hold is never both.
 */
@Entity
@Table(name = "holds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hold extends BaseEntity {

    @Column(nullable = false, updatable = false, unique = true)
    private Long transferId;

    @Column(nullable = false, updatable = false)
    private Long accountId;

    @Column(nullable = false, updatable = false)
    private Long targetAccountId;

    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private HoldStatus status;

    @Version
    private Long version;

    public static Hold place(Long transferId, Account source, Account target, BigDecimal amount) {
        Hold hold = new Hold();
        hold.transferId = transferId;
        hold.accountId = source.getId();
        hold.targetAccountId = target.getId();
        hold.amount = amount;
        hold.status = HoldStatus.HELD;
        return hold;
    }

    public void markCaptured() {
        requireHeld("capture");
        this.status = HoldStatus.CAPTURED;
    }

    public void markReleased() {
        requireHeld("release");
        this.status = HoldStatus.RELEASED;
    }

    private void requireHeld(String action) {
        if (status != HoldStatus.HELD) {
            throw new InvalidHoldStateException(transferId, status.name(), action);
        }
    }
}
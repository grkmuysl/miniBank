package com.gorkemuysal.minibank.ledger;

import com.gorkemuysal.minibank.account.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * An immutable ledger entry. Postings are never updated or deleted;
 * a wrong movement is corrected by writing a reverse posting.
 */
@Entity
@Table(name = "postings")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Posting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID of the transfer in transfer-service.
    @Column(nullable = false, updatable = false)
    private Long transferId;

    @Column(nullable = false, updatable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 6)
    private PostingDirection direction;

    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Must be called AFTER account.withdraw(...), so balanceAfter reflects the new balance. */
    public static Posting debit(Long transferId, Account account, BigDecimal amount) {
        return new Posting(transferId, account, PostingDirection.DEBIT, amount);
    }

    /** Must be called AFTER account.deposit(...), so balanceAfter reflects the new balance. */
    public static Posting credit(Long transferId, Account account, BigDecimal amount) {
        return new Posting(transferId, account, PostingDirection.CREDIT, amount);
    }

    private Posting(Long transferId, Account account, PostingDirection direction, BigDecimal amount) {
        this.transferId = transferId;
        this.accountId = account.getId();
        this.direction = direction;
        this.amount = amount;
        this.balanceAfter = account.getBalance();
    }
}
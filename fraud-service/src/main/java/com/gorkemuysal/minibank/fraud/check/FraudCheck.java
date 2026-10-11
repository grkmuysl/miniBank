package com.gorkemuysal.minibank.fraud.check;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/** One immutable fraud decision. Never updated: a decision is made once and stands. */
@Entity
@Table(name = "fraud_checks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FraudCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false, unique = true)
    private Long transferId;

    @Column(nullable = false, updatable = false)
    private String initiatorId;

    @Column(nullable = false, updatable = false, length = 26)
    private String sourceIban;

    @Column(nullable = false, updatable = false, length = 26)
    private String targetIban;

    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 10)
    private Decision decision;

    @Column(updatable = false, length = 500)
    private String reasons;

    @Column(nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Approved if no rule fired; otherwise rejected with ALL triggered rule codes. */
    public static FraudCheck decide(FraudCheckContext ctx, List<String> triggeredRules) {
        FraudCheck check = new FraudCheck();
        check.transferId = ctx.transferId();
        check.initiatorId = ctx.initiatorId();
        check.sourceIban = ctx.sourceIban();
        check.targetIban = ctx.targetIban();
        check.amount = ctx.amount();
        check.currency = ctx.currency();
        check.requestedAt = ctx.requestedAt();
        check.createdAt = Instant.now();
        if (triggeredRules.isEmpty()) {
            check.decision = Decision.APPROVED;
        } else {
            check.decision = Decision.REJECTED;
            check.reasons = String.join(",", triggeredRules);
        }
        return check;
    }

    public List<String> reasonList() {
        return reasons == null ? List.of() : Arrays.asList(reasons.split(","));
    }
}
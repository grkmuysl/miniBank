package com.gorkemuysal.minibank.fraud.rules;

import com.gorkemuysal.minibank.fraud.check.FraudCheckContext;
import com.gorkemuysal.minibank.fraud.check.FraudCheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * Too many transfers by one customer in a short time.
 * <p>
 * Counted relative to the transfer's creation time (requestedAt), not "now": after an outage,
 * a backlog of normal transfers processed within one second must not look like a burst.
 * <p>
 * Correctness relies on fraud.check-requests being keyed by initiatorId: all checks of one
 * customer are processed one after another, so each count sees all previous decisions.
 */
@Component
@RequiredArgsConstructor
public class VelocityRule implements FraudRule {

    static final String CODE = "VELOCITY_LIMIT";

    private final FraudCheckRepository fraudCheckRepository;
    private final FraudProperties properties;

    @Override
    public Optional<String> evaluate(FraudCheckContext ctx) {
        Instant to = ctx.requestedAt();
        Instant from = to.minus(properties.velocity().window());
        long previous = fraudCheckRepository.countInWindow(ctx.initiatorId(), from, to);
        return previous >= properties.velocity().maxChecks() ? Optional.of(CODE) : Optional.empty();
    }
}
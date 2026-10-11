package com.gorkemuysal.minibank.fraud.rules;

import com.gorkemuysal.minibank.fraud.check.FraudCheckContext;
import com.gorkemuysal.minibank.fraud.check.FraudCheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
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

    /**
     * Too many transfers by one customer close together in time.
     * <p>
     * Counted around the transfer's creation time (requestedAt), not "now": a backlog of normal
     * transfers processed after an outage must not look like a burst.
     * <p>
     * The window is two-sided ([t - w, t + w)) on purpose. Checks are NOT processed in creation
     * order: replies from account-service come back in any order, so a later transfer may be
     * checked first. A one-sided window would miss those, and 4 parallel transfers could all pass.
     * Two-sided, whatever the processing order, the last of N close transfers sees the other N-1.
     */
    @Override
    public Optional<String> evaluate(FraudCheckContext ctx) {
        Duration window = properties.velocity().window();
        Instant from = ctx.requestedAt().minus(window);
        Instant to = ctx.requestedAt().plus(window);
        long nearby = fraudCheckRepository.countInWindow(ctx.initiatorId(), from, to);
        return nearby >= properties.velocity().maxChecks() ? Optional.of(CODE) : Optional.empty();
    }
}
package com.gorkemuysal.minibank.transfer.retry;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Exponential backoff policy for transfers whose outcome is unknown.
 * Defaults (5s base, 10m cap, 8 attempts) give retries after 5, 10, 20, 40, 80, 160, 320 seconds,
 * i.e. about 10 minutes before a transfer is handed over to MANUAL_REVIEW.
 */
@ConfigurationProperties("transfer.retry")
public record RetryProperties(int maxAttempts, Duration baseDelay, Duration maxDelay) {

    /** Delay before the next attempt, given how many attempts have already failed. */
    public Duration delayAfter(int failedAttemptsSoFar) {
        // Cap the exponent so the shift can never overflow; maxDelay caps the result anyway.
        long multiplier = 1L << Math.min(failedAttemptsSoFar, 20);
        Duration delay = baseDelay.multipliedBy(multiplier);
        return delay.compareTo(maxDelay) > 0 ? maxDelay : delay;
    }
}
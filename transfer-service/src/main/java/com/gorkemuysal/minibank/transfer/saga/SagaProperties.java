package com.gorkemuysal.minibank.transfer.saga;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * fraudTimeout: how long money may stay held waiting for a fraud decision.
 * stuckThreshold: after this long in CAPTURING / RELEASING, log a warning (alert in step 6).
 */
@ConfigurationProperties("transfer.saga")
public record SagaProperties(Duration fraudTimeout, Duration stuckThreshold) {
}
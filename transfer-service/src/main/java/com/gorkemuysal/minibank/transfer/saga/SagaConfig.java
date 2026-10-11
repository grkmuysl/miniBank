package com.gorkemuysal.minibank.transfer.saga;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @EnableScheduling lives here from now on: RetryConfig (3.4) is deleted in part 3,
 * and the outbox poller and the timeout job must keep running without it.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(SagaProperties.class)
public class SagaConfig {
}
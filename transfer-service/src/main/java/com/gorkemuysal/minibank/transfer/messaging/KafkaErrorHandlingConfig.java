package com.gorkemuysal.minibank.transfer.messaging; // servise göre değiştir

import com.gorkemuysal.minibank.transfer.exception.SagaInvariantViolationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.ExponentialBackOff;

import java.net.ConnectException;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;

/**
 * How a failing message is treated. Spring Boot applies this bean to every @KafkaListener.
 * <ul>
 *   <li>Poison messages and invariant violations: never retried, straight to the DLT.</li>
 *   <li>Infrastructure failures (database down): retried patiently, forever. The message is fine;
 *       it must wait for the infrastructure, not end up in the DLT with hundreds of others.</li>
 *   <li>Anything else: retried for about a minute, then the DLT.</li>
 * </ul>
 * Retries are BLOCKING: the partition waits. That keeps per-transfer order intact
 * (e.g. a RELEASE_FUNDS can never overtake its HOLD_FUNDS), which the saga relies on.
 */
@Configuration
public class KafkaErrorHandlingConfig {

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) {
        // Publishes to "<original topic>.DLT", same partition number, with error details as headers.
        var recoverer = new DeadLetterPublishingRecoverer(template);

        var handler = new DefaultErrorHandler(recoverer, limitedBackOff());
        handler.addNotRetryableExceptions(
                MalformedMessageException.class,
                SagaInvariantViolationException.class);   // fraud-service: remove this line
        // null = use the default (limited) back-off
        handler.setBackOffFunction((record, ex) -> isInfrastructureFailure(ex) ? patientBackOff() : null);
        return handler;
    }

    /** Unknown errors: 1s, 2s, 4s, 8s, 16s, 16s... until ~1 minute has passed. */
    private static BackOff limitedBackOff() {
        var backOff = new ExponentialBackOff(1_000, 2.0);
        backOff.setMaxInterval(16_000);
        backOff.setMaxElapsedTime(60_000);
        return backOff;
    }

    /**
     * Infrastructure failures: up to 30s between attempts, no time limit.
     * 30s also stays well below max.poll.interval.ms (5 min): each wait ends with a new poll,
     * so Kafka never thinks the consumer is dead and never rebalances it away.
     */
    private static BackOff patientBackOff() {
        var backOff = new ExponentialBackOff(1_000, 2.0);
        backOff.setMaxInterval(30_000);
        backOff.setMaxElapsedTime(Long.MAX_VALUE);
        return backOff;
    }

    /** Walks the cause chain: the root cause is usually several wrappers deep. */
    static boolean isInfrastructureFailure(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof DataAccessResourceFailureException
                    || t instanceof TransientDataAccessResourceException
                    || t instanceof CannotCreateTransactionException
                    || t instanceof SQLTransientConnectionException   // Hikari: no connection available
                    || t instanceof ConnectException) {
                return true;
            }
            // SQLState class 08 = connection exception (e.g. PostgreSQL closed the connection)
            if (t instanceof SQLException sql && sql.getSQLState() != null && sql.getSQLState().startsWith("08")) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }
}
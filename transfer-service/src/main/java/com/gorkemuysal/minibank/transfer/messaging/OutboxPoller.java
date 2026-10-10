package com.gorkemuysal.minibank.transfer.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Publishes PENDING outbox events to Kafka, in id order.
 * <p>
 * At-least-once: if the service crashes after Kafka acknowledged but before the row is marked
 * PUBLISHED, the message is sent again on the next run. Consumers deduplicate with their inbox.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private static final int BATCH_SIZE = 100;
    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    public void publishPending() {
        List<OutboxEvent> pending = outboxRepository.findPending(PageRequest.of(0, BATCH_SIZE));

        for (OutboxEvent event : pending) {
            try {
                // .get(): wait until the broker confirms (acks=all). Only then is it safe to mark PUBLISHED.
                kafkaTemplate.send(event.getTopic(), event.getMessageKey(), event.getPayload())
                        .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                outboxRepository.markPublished(event.getId(), Instant.now());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                // Stop at the first failure: sending later events now would break their order
                // (e.g. a CAPTURE overtaking its HOLD). Everything is retried on the next run.
                log.warn("Could not publish outbox event {} to {}: {}",
                        event.getMessageId(), event.getTopic(), e.getMessage());
                return;
            }
        }
    }
}
package com.gorkemuysal.minibank.transfer.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxWriter {

    private final OutboxRepository outboxRepository;
    private final JsonMapper jsonMapper;

    /** Message keyed by transferId: all messages about one transfer stay in order. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void write(String topic, String type, Long transferId, String requestId, Object payload) {
        writeWithKey(topic, String.valueOf(transferId), type, transferId, requestId, payload);
    }

    /**
     * Message with an explicit partition key, for topics that need a different ordering,
     * e.g. fraud.check-requests is keyed by customer so the velocity rule counts correctly.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void writeWithKey(String topic, String key, String type, Long transferId,
                             String requestId, Object payload) {
        UUID messageId = UUID.randomUUID();
        MessageEnvelope envelope = new MessageEnvelope(
                messageId, type, transferId, requestId, Instant.now(), jsonMapper.valueToTree(payload));

        outboxRepository.save(OutboxEvent.pending(
                messageId, topic, key, jsonMapper.writeValueAsString(envelope)));
    }
}
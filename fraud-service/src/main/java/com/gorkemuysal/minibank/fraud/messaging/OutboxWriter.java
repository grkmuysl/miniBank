package com.gorkemuysal.minibank.fraud.messaging;

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

    /**
     * Stores a message to be published later. MANDATORY: the message must be saved in the same
     * transaction as the business change it describes; that is the whole point of the outbox.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void write(String topic, String type, Long transferId, String requestId, Object payload) {
        UUID messageId = UUID.randomUUID();
        MessageEnvelope envelope = new MessageEnvelope(
                messageId, type, transferId, requestId, Instant.now(), jsonMapper.valueToTree(payload));

        outboxRepository.save(OutboxEvent.pending(
                messageId, topic, String.valueOf(transferId), jsonMapper.writeValueAsString(envelope)));
    }
}
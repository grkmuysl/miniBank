package com.gorkemuysal.minibank.messaging;

import com.gorkemuysal.minibank.common.RequestIdFilter;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Entry point for account.commands. Kept thin on purpose: parsing and logging context here,
 * all transactional work in AccountCommandHandler.
 * <p>
 * Offsets are committed only after this method returns normally. If it throws,
 * Spring Kafka retries the message.
 */
@Component
@RequiredArgsConstructor
public class AccountCommandListener {

    private final AccountCommandHandler handler;
    private final JsonMapper jsonMapper;

    @KafkaListener(topics = AccountTopics.COMMANDS)
    public void onCommand(String raw) {
        MessageEnvelope message = jsonMapper.readValue(raw, MessageEnvelope.class);

        // Kafka is not authenticated yet: validate the ID like an HTTP header (4.3, log injection).
        boolean bound = RequestIdFilter.isValid(message.requestId());
        if (bound) {
            MDC.put(RequestIdFilter.MDC_KEY, message.requestId());
        }
        try {
            handler.handle(message);
        } finally {
            if (bound) {
                MDC.remove(RequestIdFilter.MDC_KEY);
            }
        }
    }
}
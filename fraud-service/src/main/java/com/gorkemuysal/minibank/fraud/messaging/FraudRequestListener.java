package com.gorkemuysal.minibank.fraud.messaging;

import com.gorkemuysal.minibank.fraud.common.RequestIds;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class FraudRequestListener {

    private final FraudRequestHandler handler;
    private final JsonMapper jsonMapper;

    @KafkaListener(topics = FraudTopics.CHECK_REQUESTS)
    public void onRequest(String raw) {
        MessageEnvelope message = jsonMapper.readValue(raw, MessageEnvelope.class);

        boolean bound = RequestIds.isValid(message.requestId());
        if (bound) {
            MDC.put(RequestIds.MDC_KEY, message.requestId());
        }
        try {
            handler.handle(message);
        } finally {
            if (bound) {
                MDC.remove(RequestIds.MDC_KEY);
            }
        }
    }
}
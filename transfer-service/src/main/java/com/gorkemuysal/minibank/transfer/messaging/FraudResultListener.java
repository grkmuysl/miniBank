package com.gorkemuysal.minibank.transfer.messaging;

import com.gorkemuysal.minibank.transfer.common.RequestIdFilter;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class FraudResultListener {

    private final FraudResultHandler handler;
    private final JsonMapper jsonMapper;

    @KafkaListener(topics = FraudTopics.RESULTS)
    public void onResult(String raw) {
        MessageEnvelope message = jsonMapper.readValue(raw, MessageEnvelope.class);

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

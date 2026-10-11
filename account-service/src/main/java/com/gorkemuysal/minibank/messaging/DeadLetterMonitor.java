package com.gorkemuysal.minibank.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

/**
 * Every message in a DLT is an incident a human must look at. This listener makes each one
 * visible as an ERROR log (an alert in step 6). It never throws: a failing DLT monitor
 * must not create a DLT of its own.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterMonitor {

    private final JsonMapper jsonMapper;

    @KafkaListener(
            topics = DeadLetterTopics.ACCOUNT_COMMANDS_DLT,   // transfer-service: iki DLT
            groupId = "${spring.application.name}-dlt-monitor")
    public void onDeadLetter(ConsumerRecord<String, String> record) {
        try {
            String transferId = null;
            String requestId = null;
            try {
                JsonNode json = jsonMapper.readTree(record.value());
                transferId = json.path("transferId").asString(null);
                requestId = json.path("requestId").asString(null);
            } catch (RuntimeException ignored) {
                // The message may be exactly the unreadable JSON that brought it here.
            }

            log.error("DEAD LETTER from {}[{}]@{} transferId={} requestId={} error={}: {}",
                    header(record, KafkaHeaders.DLT_ORIGINAL_TOPIC),
                    header(record, KafkaHeaders.DLT_ORIGINAL_PARTITION),
                    header(record, KafkaHeaders.DLT_ORIGINAL_OFFSET),
                    transferId, requestId,
                    header(record, KafkaHeaders.DLT_EXCEPTION_FQCN),
                    header(record, KafkaHeaders.DLT_EXCEPTION_MESSAGE));
        } catch (RuntimeException e) {
            log.error("DEAD LETTER (could not be inspected) at {}-{}@{}",
                    record.topic(), record.partition(), record.offset(), e);
        }
    }

    private static String header(ConsumerRecord<?, ?> record, String name) {
        Header h = record.headers().lastHeader(name);
        if (h == null) {
            return null;
        }
        // Partition and offset headers are binary ints/longs, the rest are UTF-8 strings.
        byte[] v = h.value();
        if (name.equals(KafkaHeaders.DLT_ORIGINAL_PARTITION) && v.length == 4) {
            return String.valueOf(java.nio.ByteBuffer.wrap(v).getInt());
        }
        if (name.equals(KafkaHeaders.DLT_ORIGINAL_OFFSET) && v.length == 8) {
            return String.valueOf(java.nio.ByteBuffer.wrap(v).getLong());
        }
        return new String(v, StandardCharsets.UTF_8);
    }
}
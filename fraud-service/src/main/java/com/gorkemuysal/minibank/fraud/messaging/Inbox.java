package com.gorkemuysal.minibank.fraud.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class Inbox {

    private final JdbcClient jdbcClient;

    /**
     * Records the message as processed. Returns false if it was already processed.
     * <p>
     * ON CONFLICT DO NOTHING instead of catching a duplicate-key exception: in PostgreSQL
     * any error aborts the whole transaction, which would also lose the business work.
     * <p>
     * MANDATORY: must run in the caller's transaction, so that the inbox record is committed
     * or rolled back together with the business change and the outbox reply.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markProcessed(UUID messageId) {
        int inserted = jdbcClient.sql("""
                        INSERT INTO processed_messages (message_id) VALUES (:id)
                        ON CONFLICT (message_id) DO NOTHING
                        """)
                .param("id", messageId)
                .update();
        return inserted == 1;
    }
}
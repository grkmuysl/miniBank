package com.gorkemuysal.minibank.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * account-service owns these topics: they are its asynchronous API.
 * Other services (transfer-service) only produce to / consume from them.
 * Spring's KafkaAdmin creates them at startup if they don't exist yet.
 */
@Configuration
public class AccountTopics {

    public static final String COMMANDS = "account.commands";
    public static final String REPLIES = "account.replies";

    // 3 partitions: messages are keyed by transferId, so each transfer's messages
    // stay ordered within one partition while different transfers are processed in parallel.
    private static final int PARTITIONS = 3;

    // 1 replica: we run a single broker locally. In production this would be 3.
    private static final int REPLICAS = 1;

    @Bean
    NewTopic accountCommandsTopic() {
        return TopicBuilder.name(COMMANDS).partitions(PARTITIONS).replicas(REPLICAS).build();
    }

    @Bean
    NewTopic accountRepliesTopic() {
        return TopicBuilder.name(REPLIES).partitions(PARTITIONS).replicas(REPLICAS).build();
    }
}
package com.gorkemuysal.minibank.fraud.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** fraud-service owns its request and result topics; they are its asynchronous API. */
@Configuration
public class FraudTopics {

    /** Keyed by initiatorId: all checks of one customer are processed in order (velocity rule). */
    public static final String CHECK_REQUESTS = "fraud.check-requests";

    /** Keyed by transferId, like every other message about a single transfer. */
    public static final String RESULTS = "fraud.results";

    private static final int PARTITIONS = 3;
    private static final int REPLICAS = 1;

    @Bean
    NewTopic fraudCheckRequestsTopic() {
        return TopicBuilder.name(CHECK_REQUESTS).partitions(PARTITIONS).replicas(REPLICAS).build();
    }

    @Bean
    NewTopic fraudResultsTopic() {
        return TopicBuilder.name(RESULTS).partitions(PARTITIONS).replicas(REPLICAS).build();
    }
}
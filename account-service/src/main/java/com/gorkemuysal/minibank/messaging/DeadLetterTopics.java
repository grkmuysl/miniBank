package com.gorkemuysal.minibank.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class DeadLetterTopics {

    public static final String ACCOUNT_COMMANDS_DLT = AccountTopics.COMMANDS + ".DLT";

    // Same partition count as the source topic: the recoverer writes to the same partition number.
    @Bean
    NewTopic accountCommandsDlt() {
        return TopicBuilder.name(ACCOUNT_COMMANDS_DLT).partitions(3).replicas(1).build();
    }
}
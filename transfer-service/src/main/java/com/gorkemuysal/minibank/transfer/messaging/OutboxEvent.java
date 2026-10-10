package com.gorkemuysal.minibank.transfer.messaging;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false, unique = true)
    private UUID messageId;

    @Column(nullable = false, updatable = false, length = 100)
    private String topic;

    @Column(nullable = false, updatable = false, length = 100)
    private String messageKey;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OutboxStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant publishedAt;

    static OutboxEvent pending(UUID messageId, String topic, String key, String payload) {
        OutboxEvent event = new OutboxEvent();
        event.messageId = messageId;
        event.topic = topic;
        event.messageKey = key;
        event.payload = payload;
        event.status = OutboxStatus.PENDING;
        event.createdAt = Instant.now();
        return event;
    }
}
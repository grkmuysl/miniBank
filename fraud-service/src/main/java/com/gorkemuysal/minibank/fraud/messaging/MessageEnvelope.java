package com.gorkemuysal.minibank.fraud.messaging;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * Common wrapper of every message. The payload's shape depends on the type,
 * so it is kept as a JSON tree and converted once the type is known.
 * <p>
 * Each service defines its own copy of this record; the contract is the JSON shape,
 * not a shared Java class (no common module, see 3.1).
 */
public record MessageEnvelope(
        UUID messageId,
        String type,
        Long transferId,
        String requestId,
        Instant occurredAt,
        JsonNode payload
) {
}
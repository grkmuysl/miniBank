package com.gorkemuysal.minibank.fraud.messaging;

/**
 * A message that can never be processed, no matter how often it is retried:
 * unreadable JSON, missing envelope fields, unknown type, invalid payload.
 * Sent to the DLT immediately so it doesn't block its partition.
 */
public class MalformedMessageException extends RuntimeException {

    public MalformedMessageException(String message) {
        super(message);
    }

    public MalformedMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
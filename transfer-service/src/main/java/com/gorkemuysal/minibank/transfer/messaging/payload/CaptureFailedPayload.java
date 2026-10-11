package com.gorkemuysal.minibank.transfer.messaging.payload;

public record CaptureFailedPayload(String code, String message) {
}

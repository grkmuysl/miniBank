package com.gorkemuysal.minibank.messaging.payload;

public record CaptureFailedPayload(String code, String message) {
}
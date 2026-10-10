package com.gorkemuysal.minibank.transfer.messaging;

/** code: the same error codes as the REST API (INSUFFICIENT_BALANCE, ACCOUNT_NOT_FOUND, ...). */
public record HoldFailedPayload(String code, String message) {
}
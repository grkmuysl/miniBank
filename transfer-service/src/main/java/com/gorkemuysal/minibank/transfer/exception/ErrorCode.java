package com.gorkemuysal.minibank.transfer.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Invalid request"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Request body could not be read"),
    MISSING_HEADER(HttpStatus.BAD_REQUEST, "Header is missing"),
    INVALID_IDEMPOTENCY_KEY(HttpStatus.BAD_REQUEST, "Idempotency-Key cannot be empty and it can be at least 64 chars"),
    SAME_IBAN_TRANSFER(HttpStatus.BAD_REQUEST, "Source iban and target iban cannot be same"),
    TRANSFER_NOT_FOUND(HttpStatus.NOT_FOUND, "Transfer not found"),
    IDEMPOTENCY_KEY_REUSED(HttpStatus.UNPROCESSABLE_CONTENT, "This Idempotency-Key has already been used in a different request.");

    private final HttpStatus status;
    private final String defaultMessage;
}
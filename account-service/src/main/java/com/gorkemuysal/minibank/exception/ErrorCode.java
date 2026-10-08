package com.gorkemuysal.minibank.exception;


import org.springframework.http.HttpStatus;

public enum ErrorCode {
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND),
    INSUFFICIENT_BALANCE(HttpStatus.UNPROCESSABLE_CONTENT),
    ACCOUNT_NOT_ACTIVE(HttpStatus.UNPROCESSABLE_CONTENT),
    CURRENCY_MISMATCH(HttpStatus.UNPROCESSABLE_CONTENT),
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST),
    HOLD_NOT_FOUND(HttpStatus.NOT_FOUND),
    INVALID_HOLD_STATE(HttpStatus.CONFLICT);


    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
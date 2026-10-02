package com.gorkemuysal.minibank.exception;

public class CurrencyMismatchException extends BusinessException {
    public CurrencyMismatchException(String message) {
        super(ErrorCode.CURRENCY_MISMATCH, message);
    }
}
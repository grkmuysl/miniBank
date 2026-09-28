package com.gorkemuysal.minibank.exception;

public class InvalidAmountException extends BusinessException {
    public InvalidAmountException(String message) {
        super(ErrorCode.INVALID_AMOUNT, message);
    }
}

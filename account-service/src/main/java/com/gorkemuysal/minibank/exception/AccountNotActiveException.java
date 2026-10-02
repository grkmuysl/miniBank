package com.gorkemuysal.minibank.exception;

public class AccountNotActiveException extends BusinessException {
    public AccountNotActiveException(String message) {
        super(ErrorCode.ACCOUNT_NOT_ACTIVE, message);
    }
}
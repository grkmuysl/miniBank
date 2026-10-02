package com.gorkemuysal.minibank.exception;

public class AccountNotFoundException extends BusinessException {
    public AccountNotFoundException(Long id) {
        super(ErrorCode.ACCOUNT_NOT_FOUND, "Account not found: " + id);
    }
}
package com.gorkemuysal.minibank.exception;

public class HoldNotFoundException extends BusinessException {
    public HoldNotFoundException(Long transferId) {
        super(ErrorCode.HOLD_NOT_FOUND, "No hold found for transfer " + transferId);
    }
}
package com.gorkemuysal.minibank.exception;

public class InvalidHoldStateException extends BusinessException {
    public InvalidHoldStateException(Long transferId, String currentStatus, String action) {
        super(ErrorCode.INVALID_HOLD_STATE,
                "Cannot " + action + " hold of transfer " + transferId + ": it is already " + currentStatus);
    }
}
package com.gorkemuysal.minibank.hold.dto;

import com.gorkemuysal.minibank.hold.Hold;
import com.gorkemuysal.minibank.hold.HoldStatus;

import java.math.BigDecimal;

public record HoldResponse(Long transferId, HoldStatus status, BigDecimal amount) {

    public static HoldResponse from(Hold hold) {
        return new HoldResponse(hold.getTransferId(), hold.getStatus(), hold.getAmount());
    }
}
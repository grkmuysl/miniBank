package com.gorkemuysal.minibank.messaging.payload;

import java.math.BigDecimal;

public record FundsHeldPayload(BigDecimal amount) {
}
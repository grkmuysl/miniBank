package com.gorkemuysal.minibank.transfer.messaging;

import java.math.BigDecimal;

public record FundsHeldPayload(BigDecimal amount) {
}
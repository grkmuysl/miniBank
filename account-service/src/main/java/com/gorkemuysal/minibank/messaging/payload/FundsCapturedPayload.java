package com.gorkemuysal.minibank.messaging.payload;

import java.math.BigDecimal;

public record FundsCapturedPayload(BigDecimal amount) {
}
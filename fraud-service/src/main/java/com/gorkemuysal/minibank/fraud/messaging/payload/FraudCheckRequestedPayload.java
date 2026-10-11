package com.gorkemuysal.minibank.fraud.messaging.payload;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

/** requestedAt: when the transfer was created. Rules use it as "now" (see VelocityRule). */
public record FraudCheckRequestedPayload(
        @NotBlank String initiatorId,
        @NotNull @Pattern(regexp = "TR\\d{24}") String sourceIban,
        @NotNull @Pattern(regexp = "TR\\d{24}") String targetIban,
        @NotNull @Positive BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull Instant requestedAt
) {
}
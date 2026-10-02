package com.gorkemuysal.minibank.transfer.dto;

import com.gorkemuysal.minibank.transfer.Currency;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateTransferRequest(
        @NotNull @Pattern(regexp = "TR\\d{24}") String sourceIban,
        @NotNull @Pattern(regexp = "TR\\d{24}") String targetIban,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotNull Currency currency,
        @Size(max = 140) String description
) {
}
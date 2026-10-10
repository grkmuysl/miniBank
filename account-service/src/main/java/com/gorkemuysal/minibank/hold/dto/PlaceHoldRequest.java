package com.gorkemuysal.minibank.hold.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gorkemuysal.minibank.account.Currency;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PlaceHoldRequest(
        @NotNull @Positive Long transferId,
        @NotBlank String initiatorId,
        @NotNull @Pattern(regexp = "TR\\d{24}") String sourceIban,
        @NotNull @Pattern(regexp = "TR\\d{24}") String targetIban,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotNull Currency currency
) {
    @JsonIgnore
    @AssertTrue(message = "Source and target accounts must be different")
    public boolean isDistinctAccounts() {
        return sourceIban == null || !sourceIban.equals(targetIban);
    }
}
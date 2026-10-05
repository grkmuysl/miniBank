package com.gorkemuysal.minibank.ledger.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gorkemuysal.minibank.account.Currency;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreatePostingRequest(
        @NotNull @Positive Long transferId,
        @NotBlank String initiatorId,
        @NotNull @Pattern(regexp = "TR\\d{24}") String sourceIban,
        @NotNull @Pattern(regexp = "TR\\d{24}") String targetIban,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotNull Currency currency
) {
    /**
     * transfer-service already rejects same-IBAN transfers, but an internal API
     * must not trust its callers blindly. Violations end up as 400 VALIDATION_ERROR.
     */
    @JsonIgnore
    @AssertTrue(message = "Source and target accounts must be different")
    public boolean isDistinctAccounts() {
        return sourceIban == null || !sourceIban.equals(targetIban);
    }
}
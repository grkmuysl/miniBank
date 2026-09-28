package com.gorkemuysal.minibank.account.dto;

import com.gorkemuysal.minibank.account.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank @Size(max = 100) String ownerName,
        @NotNull Currency currency
) {}
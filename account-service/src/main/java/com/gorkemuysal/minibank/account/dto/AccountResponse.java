package com.gorkemuysal.minibank.account.dto;

import com.gorkemuysal.minibank.account.Account;
import com.gorkemuysal.minibank.account.AccountStatus;
import com.gorkemuysal.minibank.account.Currency;

import java.math.BigDecimal;
import java.time.Instant;

// A record type for the represt account response objectss
public record AccountResponse(
        Long id,
        String accountNumber,
        String ownerName,
        Currency currency,
        BigDecimal balance,
        AccountStatus status,
        Instant createdAt,
        Instant updatedAts
) {

    /**
     * A helper function for generate a Account response object from Account object wich returns from DB
     *
     * @param An Account object
     * @return AccountResponse object.
     * **/
    public static AccountResponse from(Account account){
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getOwnerName(),
                account.getCurrency(),
                account.getBalance(),
                account.getStatus(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}

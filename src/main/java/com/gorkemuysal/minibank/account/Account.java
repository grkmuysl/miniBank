package com.gorkemuysal.minibank.account;

import com.gorkemuysal.minibank.common.BaseEntity;
import com.gorkemuysal.minibank.exception.AccountNotActiveException;
import com.gorkemuysal.minibank.exception.CurrencyMismatchException;
import com.gorkemuysal.minibank.exception.InsufficientBalanceException;
import com.gorkemuysal.minibank.exception.InvalidAmountException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account extends BaseEntity {

    @Column(nullable = false, unique = true, updatable = false, length = 26)
    private String accountNumber;

    @Column(nullable = false, length = 100)
    private String ownerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 3)
    private Currency currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Version
    private Long version;

    private Account(String accountNumber, String ownerName, Currency currency) {
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.currency = currency;
        this.balance = BigDecimal.ZERO.setScale(2);
        this.status = AccountStatus.ACTIVE;
    }

    // Handle open new account
    public static Account open(String accountNumber, String ownerName, Currency currency) {
        return new Account(accountNumber, ownerName, currency);
    }

    // handle deposit specific amount and currency
    public void deposit(BigDecimal amount, Currency currency){
        ensureValidAmount(amount);
        ensureActive();
        ensureSameCurrency(currency);
        this.balance = this.balance.add(amount);
    }

    // handle withdraw operations with specific amount and currency
    public void withdraw(BigDecimal amount , Currency currency){
        ensureValidAmount(amount);
        ensureActive();
        ensureSameCurrency(currency);
        ensureSufficientBalance(amount);
        this.balance = this.balance.subtract(amount);
    }

    /**
     * checks if account is active or not.
     *
     * @throws AccountNotActiveException if account is not active
     * */
    private void ensureActive(){
        if(status != AccountStatus.ACTIVE){
            throw new AccountNotActiveException("Account is not active.");
        }
    }

    /**
     * checks currency match.
     *
     * @throws CurrencyMismatchException if there is a mismatch
     * */
    private void ensureSameCurrency(Currency currency) {
        if (currency != this.currency) {
            throw new CurrencyMismatchException(
                    "Currency mismatch: account is " + this.currency + ", request is " + currency);
        }
    }

    /**
     * Checks balance is sufficeint or not.
     *
     * @throws InsufficientBalanceException if balance is insufficient
     * */
    private void ensureSufficientBalance(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance.");
        }
    }

    /**
     * Checks is valid amount.
     *
     * @throws InvalidAmountException if amount is not valid
     * */
    private void ensureValidAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero.");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new InvalidAmountException("Amount cannot have more than 2 decimal places.");
        }
    }
}
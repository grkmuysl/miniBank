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

    @Column(name = "owner_id", nullable = false, updatable = false)
    private String ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 3)
    private Currency currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal heldAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Version
    private Long version;

    private Account(String accountNumber, String ownerId, String ownerName, Currency currency) {
        this.accountNumber = accountNumber;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.currency = currency;
        this.balance = BigDecimal.ZERO.setScale(2);
        this.heldAmount = BigDecimal.ZERO.setScale(2);
        this.status = AccountStatus.ACTIVE;
    }

    // Handle open new account
    public static Account open(String accountNumber,String ownerId ,String ownerName, Currency currency) {
        return new Account(accountNumber, ownerId, ownerName, currency);
    }

    // handle deposit specific amount and currency
    public void deposit(BigDecimal amount, Currency currency){
        ensureValidAmount(amount);
        ensureActive();
        ensureSameCurrency(currency);
        this.balance = this.balance.add(amount);
    }

    // handle withdraw operations with specific amount and currency
    public void withdraw(BigDecimal amount, Currency currency) {
        ensureValidAmount(amount);
        ensureActive();
        ensureSameCurrency(currency);
        ensureSufficientAvailableBalance(amount);   // held money cannot be withdrawn
        this.balance = this.balance.subtract(amount);
    }

    /** Money that can actually be spent: total balance minus what is reserved by holds. */
    public BigDecimal getAvailableBalance() {
        return balance.subtract(heldAmount);
    }

    /**
     * Reserves money for a pending transfer. The balance does not change, but the held
     * part can no longer be withdrawn or reserved again. Reversible via releaseHold.
     */
    public void hold(BigDecimal amount, Currency currency) {
        ensureValidAmount(amount);
        ensureActive();
        ensureSameCurrency(currency);
        ensureSufficientAvailableBalance(amount);
        this.heldAmount = this.heldAmount.add(amount);
    }

    /**
     * Turns a hold into a real debit: the money leaves the account. Irreversible.
     * Requires an active account; if this fails, the saga releases the hold instead.
     */
    public void captureHold(BigDecimal amount) {
        ensureActive();
        ensureHeldAtLeast(amount);
        this.heldAmount = this.heldAmount.subtract(amount);
        this.balance = this.balance.subtract(amount);
    }

    /**
     * Cancels a hold; the money becomes available again.
     * Deliberately has NO business checks (e.g. account status): this is a compensation step
     * and must always succeed, otherwise the customer's money would stay blocked forever.
     */
    public void releaseHold(BigDecimal amount) {
        ensureHeldAtLeast(amount);
        this.heldAmount = this.heldAmount.subtract(amount);
    }

    // handle convert status of account from active to frozen.
    public void freeze(){
        ensureActive();
        this.status = AccountStatus.FROZEN;
    }

    // handle convert status of account from froze to active.
    public void active(){
        ensureFrozen();
        this.status = AccountStatus.ACTIVE;
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
     * checks if account is frozen or not.
     *
     * @throws AccountNotActiveException if account is not active
     * */
    private void ensureFrozen(){
        if(status != AccountStatus.FROZEN){
            throw new AccountNotActiveException("Account is not frozen.");
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
     * Checks that enough unreserved money is available.
     *
     * @throws InsufficientBalanceException if available balance is insufficient
     */
    private void ensureSufficientAvailableBalance(BigDecimal amount) {
        if (getAvailableBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient available balance.");
        }
    }

    /**
     * Internal invariant: we can only capture or release what is actually held.
     * A violation means a bug (e.g. the same hold processed twice), not a customer error.
     */
    private void ensureHeldAtLeast(BigDecimal amount) {
        if (heldAmount.compareTo(amount) < 0) {
            throw new IllegalStateException(
                    "Cannot release/capture " + amount + ": only " + heldAmount + " is held on account " + getId());
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
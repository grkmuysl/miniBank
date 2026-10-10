package com.gorkemuysal.minibank.account;

import com.gorkemuysal.minibank.exception.AccountNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The ONE place that defines how accounts are locked.
 * <p>
 * Ordered locking only prevents deadlocks if every code path uses the same order.
 * If postings locked by IBAN and captures by ID, A->B and B->A could deadlock again.
 * Rule: when locking two accounts, always lock the alphabetically smaller IBAN first.
 * <p>
 * Must be called inside a transaction; locks are held until it ends.
 */
@Component
@RequiredArgsConstructor
public class AccountLocker {

    private final AccountRepository accountRepository;

    public Account lock(String iban) {
        return accountRepository.findByAccountNumberForUpdate(iban)
                .orElseThrow(() -> new AccountNotFoundException(iban));
    }

    /** Locks both accounts in IBAN order and returns them in source/target order. */
    public LockedPair lockPair(String sourceIban, String targetIban) {
        boolean sourceFirst = sourceIban.compareTo(targetIban) < 0;
        Account first = lock(sourceFirst ? sourceIban : targetIban);
        Account second = lock(sourceFirst ? targetIban : sourceIban);
        return sourceFirst ? new LockedPair(first, second) : new LockedPair(second, first);
    }

    public record LockedPair(Account source, Account target) {
    }
}
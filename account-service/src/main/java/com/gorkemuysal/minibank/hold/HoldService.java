package com.gorkemuysal.minibank.hold;

import com.gorkemuysal.minibank.account.Account;
import com.gorkemuysal.minibank.account.AccountLocker;
import com.gorkemuysal.minibank.account.AccountRepository;
import com.gorkemuysal.minibank.account.AccountStatus;
import com.gorkemuysal.minibank.exception.AccountNotActiveException;
import com.gorkemuysal.minibank.exception.AccountNotFoundException;
import com.gorkemuysal.minibank.exception.CurrencyMismatchException;
import com.gorkemuysal.minibank.exception.HoldNotFoundException;
import com.gorkemuysal.minibank.hold.dto.HoldResponse;
import com.gorkemuysal.minibank.hold.dto.PlaceHoldRequest;
import com.gorkemuysal.minibank.ledger.Posting;
import com.gorkemuysal.minibank.ledger.PostingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hold lifecycle: place -> capture | release. All three operations are idempotent per transferId,
 * because in 5.2 Kafka may deliver the same command more than once.
 * <p>
 * Pattern in every method: fast-path check without locks, then take locks,
 * then re-check the hold's state, then act.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HoldService {

    private final HoldRepository holdRepository;
    private final AccountRepository accountRepository;
    private final AccountLocker accountLocker;
    private final PostingRepository postingRepository;

    @Transactional
    public HoldResponse placeHold(PlaceHoldRequest request) {
        // 1) Already placed? (any state counts: a released hold must not be placed again)
        var existing = holdRepository.findByTransferId(request.transferId());
        if (existing.isPresent()) {
            return HoldResponse.from(existing.get());
        }

        // 2) Validate the target now: catching problems before money is held
        //    means no compensation is needed later.
        Account target = accountRepository.findByAccountNumber(request.targetIban())
                .orElseThrow(() -> new AccountNotFoundException(request.targetIban()));
        if (target.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException("Target account is not active.");
        }
        if (target.getCurrency() != request.currency()) {
            throw new CurrencyMismatchException(
                    "Currency mismatch: target account is " + target.getCurrency() + ", transfer is " + request.currency());
        }

        // 3) Lock only the source: placing a hold changes nothing on the target.
        Account source = accountLocker.lock(request.sourceIban());

        // 4) Re-check after the lock: a concurrent call with the same transferId may have just committed.
        existing = holdRepository.findByTransferId(request.transferId());
        if (existing.isPresent()) {
            return HoldResponse.from(existing.get());
        }

        // 5) Ownership (same rule and same "not found" disguise as in 3.3).
        if (!source.getOwnerId().equals(request.initiatorId())) {
            throw new AccountNotFoundException(request.sourceIban());
        }

        // 6) Domain rules: active, currency, available balance.
        source.hold(request.amount(), request.currency());

        Hold hold = holdRepository.saveAndFlush(Hold.place(request.transferId(), source, target, request.amount()));
        log.info("Hold placed for transfer {}: {} {} on {}", request.transferId(),
                request.amount(), request.currency(), request.sourceIban());
        return HoldResponse.from(hold);
    }

    @Transactional
    public HoldResponse capture(Long transferId) {
        // 1) Unmanaged read: we need the account IDs to lock, but must not cache the entity yet.
        HoldRef ref = holdRepository.findRefByTransferId(transferId)
                .orElseThrow(() -> new HoldNotFoundException(transferId));
        if (ref.status() == HoldStatus.CAPTURED) {
            return HoldResponse.from(holdRepository.findByTransferId(transferId).orElseThrow());
        }

        // 2) Lock both accounts in the global IBAN order.
        String sourceIban = accountRepository.findAccountNumberById(ref.accountId()).orElseThrow();
        String targetIban = accountRepository.findAccountNumberById(ref.targetAccountId()).orElseThrow();
        AccountLocker.LockedPair accounts = accountLocker.lockPair(sourceIban, targetIban);

        // 3) Lock and load the hold itself; this is its first load in this transaction, so it is fresh.
        Hold hold = holdRepository.findByTransferIdForUpdate(transferId).orElseThrow();
        switch (hold.getStatus()) {
            case CAPTURED -> {
                return HoldResponse.from(hold);          // a concurrent call finished first
            }
            case RELEASED -> hold.markCaptured();         // throws INVALID_HOLD_STATE
            case HELD -> {
                // Irreversible step: money leaves the source and arrives at the target.
                // If either account check fails, the exception rolls back everything.
                accounts.source().captureHold(hold.getAmount());
                accounts.target().deposit(hold.getAmount(), accounts.source().getCurrency());

                postingRepository.saveAndFlush(Posting.debit(transferId, accounts.source(), hold.getAmount()));
                postingRepository.saveAndFlush(Posting.credit(transferId, accounts.target(), hold.getAmount()));

                hold.markCaptured();
                log.info("Hold captured for transfer {}", transferId);
            }
        }
        return HoldResponse.from(hold);
    }

    @Transactional
    public HoldResponse release(Long transferId) {
        HoldRef ref = holdRepository.findRefByTransferId(transferId)
                .orElseThrow(() -> new HoldNotFoundException(transferId));
        if (ref.status() == HoldStatus.RELEASED) {
            return HoldResponse.from(holdRepository.findByTransferId(transferId).orElseThrow());
        }

        // Only the source account changes, so only it is locked.
        String sourceIban = accountRepository.findAccountNumberById(ref.accountId()).orElseThrow();
        Account source = accountLocker.lock(sourceIban);

        Hold hold = holdRepository.findByTransferIdForUpdate(transferId).orElseThrow();
        switch (hold.getStatus()) {
            case RELEASED -> {
                return HoldResponse.from(hold);
            }
            case CAPTURED -> hold.markReleased();          // throws INVALID_HOLD_STATE
            case HELD -> {
                // Compensation: no business checks, must always succeed (see Account.releaseHold).
                source.releaseHold(hold.getAmount());
                hold.markReleased();
                log.info("Hold released for transfer {}", transferId);
            }
        }
        return HoldResponse.from(hold);
    }
}
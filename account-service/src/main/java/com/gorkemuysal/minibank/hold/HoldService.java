package com.gorkemuysal.minibank.hold;

import com.gorkemuysal.minibank.account.Account;
import com.gorkemuysal.minibank.account.AccountLocker;
import com.gorkemuysal.minibank.account.AccountRepository;
import com.gorkemuysal.minibank.account.AccountStatus;
import com.gorkemuysal.minibank.exception.*;
import com.gorkemuysal.minibank.hold.dto.HoldResponse;
import com.gorkemuysal.minibank.hold.dto.PlaceHoldRequest;
import com.gorkemuysal.minibank.ledger.Posting;
import com.gorkemuysal.minibank.ledger.PostingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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

    /** REST path: business failures are thrown and mapped to HTTP errors by GlobalExceptionHandler. */
    @Transactional
    public HoldResponse placeHold(PlaceHoldRequest request) {
        return HoldResponse.from(doPlaceHold(request));
    }

    /**
     * Messaging path: business failures are returned as {@link HoldResult.Rejected}.
     * <p>
     * Why: the Kafka handler's transaction (inbox + this + outbox) must commit even when the hold
     * is rejected, so that the HOLD_FAILED reply and the inbox record are saved. If a
     * BusinessException crossed this method's transactional proxy, Spring would mark the shared
     * transaction rollback-only and the commit would fail with UnexpectedRollbackException.
     * Catching it here, before it reaches the proxy, keeps the transaction healthy.
     * <p>
     * This is only safe because doPlaceHold performs ALL business checks before its single
     * state change. A rejected hold must leave nothing behind; keep that order when editing.
     * Technical errors (database down, ...) are not caught: they must roll everything back.
     */
    @Transactional
    public HoldResult tryPlaceHold(PlaceHoldRequest request) {
        try {
            Hold hold = doPlaceHold(request);
            return new HoldResult.Held(hold.getId(), hold.getAmount());
        } catch (BusinessException e) {
            log.info("Hold rejected for transfer {}: {}", request.transferId(), e.getErrorCode());
            return new HoldResult.Rejected(e.getErrorCode().name(), e.getMessage());
        }
    }

    private Hold doPlaceHold(PlaceHoldRequest request){
        // 1) Already placed? (any state counts: a released hold must not be placed again)
        var existing = holdRepository.findByTransferId(request.transferId());
        if (existing.isPresent()) {
            return existing.get();
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
            return existing.get();
        }

        // 5) Ownership (same rule and same "not found" disguise as in 3.3).
        if (!source.getOwnerId().equals(request.initiatorId())) {
            throw new AccountNotFoundException(request.sourceIban());
        }

        // 6) The ONLY state change, after every business check (see tryPlaceHold).
        source.hold(request.amount(), request.currency());

        Hold hold = holdRepository.saveAndFlush(Hold.place(request.transferId(), source, target, request.amount()));
        log.info("Hold placed for transfer {}: {} {} on {}", request.transferId(),
                request.amount(), request.currency(), request.sourceIban());
        return hold;
    }

    @Transactional
    public HoldResponse capture(Long transferId) {
        return HoldResponse.from(doCapture(transferId));
    }

    /**
     * Messaging path: business failures become {@link CaptureResult.Rejected}.
     * Safe ONLY because doCapture validates both accounts before changing either (see there).
     */
    @Transactional
    public CaptureResult tryCapture(Long transferId) {
        try {
            Hold hold = doCapture(transferId);
            return new CaptureResult.Captured(hold.getAmount());
        } catch (BusinessException e) {
            log.warn("Capture rejected for transfer {}: {}", transferId, e.getErrorCode());
            return new CaptureResult.Rejected(e.getErrorCode().name(), e.getMessage());
        }
    }

    private Hold doCapture(Long transferId) {
        HoldRef ref = holdRepository.findRefByTransferId(transferId)
                .orElseThrow(() -> new HoldNotFoundException(transferId));
        if (ref.status() == HoldStatus.CAPTURED) {
            return holdRepository.findByTransferId(transferId).orElseThrow();
        }

        String sourceIban = accountRepository.findAccountNumberById(ref.accountId()).orElseThrow();
        String targetIban = accountRepository.findAccountNumberById(ref.targetAccountId()).orElseThrow();
        AccountLocker.LockedPair accounts = accountLocker.lockPair(sourceIban, targetIban);

        Hold hold = holdRepository.findByTransferIdForUpdate(transferId).orElseThrow();
        switch (hold.getStatus()) {
            case CAPTURED -> {
                return hold;
            }
            case RELEASED -> hold.markCaptured();   // throws INVALID_HOLD_STATE, nothing changed yet
            case HELD -> {
                BigDecimal amount = hold.getAmount();

                // VALIDATE BOTH SIDES FIRST. tryCapture turns business errors into a reply and
                // COMMITS. If deposit failed after captureHold had already run, the debit would be
                // committed without the credit: money would vanish. After these two lines,
                // no business rule can fail anymore.
                accounts.source().verifyCanCaptureHold(amount);
                accounts.target().verifyCanReceive(amount, accounts.source().getCurrency());

                accounts.source().captureHold(amount);
                accounts.target().deposit(amount, accounts.source().getCurrency());

                postingRepository.saveAndFlush(Posting.debit(transferId, accounts.source(), amount));
                postingRepository.saveAndFlush(Posting.credit(transferId, accounts.target(), amount));

                hold.markCaptured();
                log.info("Hold captured for transfer {}", transferId);
            }
        }
        return hold;
    }

    @Transactional
    public HoldResponse release(Long transferId) {
        return HoldResponse.from(doRelease(transferId));
    }

    /**
     * Messaging path for the saga's compensation. Must never fail for a business reason:
     * <ul>
     *   <li>HELD: released.</li>
     *   <li>RELEASED: already done, nothing to do.</li>
     *   <li>No hold at all (it was never placed): nothing to release, still a success.</li>
     *   <li>CAPTURED: an inconsistency, a bug in the saga. Thrown as a TECHNICAL error so that it
     *       is retried and, from 5.5 on, ends up in the DLT. A compensation is never silently swallowed.</li>
     * </ul>
     */
    @Transactional
    public void releaseForCompensation(Long transferId) {
        try {
            doRelease(transferId);
        } catch (HoldNotFoundException e) {
            log.info("Release for transfer {}: no hold was ever placed, nothing to release", transferId);
        } catch (InvalidHoldStateException e) {
            log.error("SAGA INCONSISTENCY: release requested for transfer {} whose hold is already CAPTURED",
                    transferId);
            throw new IllegalStateException("Cannot release a captured hold, transfer " + transferId, e);
        }
    }

    /**
     * Shared by the REST path (release) and the saga (releaseForCompensation).
     * Throws HoldNotFoundException if no hold exists, InvalidHoldStateException if it is CAPTURED.
     * Changes nothing before the hold's state has been checked under lock.
     */
    private Hold doRelease(Long transferId) {
        // 1) Unmanaged read: we need the source account to lock, without caching the Hold entity yet.
        HoldRef ref = holdRepository.findRefByTransferId(transferId)
                .orElseThrow(() -> new HoldNotFoundException(transferId));
        if (ref.status() == HoldStatus.RELEASED) {
            return holdRepository.findByTransferId(transferId).orElseThrow();
        }

        // 2) Only the source account changes, so only it is locked (same global IBAN rule).
        String sourceIban = accountRepository.findAccountNumberById(ref.accountId()).orElseThrow();
        Account source = accountLocker.lock(sourceIban);

        // 3) Lock and load the hold; first load in this transaction, so its state is fresh.
        Hold hold = holdRepository.findByTransferIdForUpdate(transferId).orElseThrow();
        switch (hold.getStatus()) {
            case RELEASED -> {
                return hold;                       // a concurrent call finished first
            }
            case CAPTURED -> hold.markReleased();  // throws INVALID_HOLD_STATE, nothing changed
            case HELD -> {
                // Compensation: no business checks (see Account.releaseHold); must always succeed.
                source.releaseHold(hold.getAmount());
                hold.markReleased();
                log.info("Hold released for transfer {}", transferId);
            }
        }
        return hold;
    }
}
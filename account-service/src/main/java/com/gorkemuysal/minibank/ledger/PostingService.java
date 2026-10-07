package com.gorkemuysal.minibank.ledger;

import com.gorkemuysal.minibank.account.Account;
import com.gorkemuysal.minibank.account.AccountRepository;
import com.gorkemuysal.minibank.exception.AccountNotFoundException;
import com.gorkemuysal.minibank.ledger.dto.CreatePostingRequest;
import com.gorkemuysal.minibank.ledger.dto.PostingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostingService {

    private final AccountRepository accountRepository;
    private final PostingRepository postingRepository;

    /**
     * Moves money between two accounts and records it in the ledger, atomically.
     * Either both balances change and both postings are written, or nothing happens.
     */
    @Transactional
    public PostingResponse post(CreatePostingRequest request) {
        // 1) Fast path: already posted? Avoids taking locks for obvious retries.
        List<Posting> existing = postingRepository.findAllByTransferId(request.transferId());
        if (!existing.isEmpty()) {
            return PostingResponse.from(request.transferId(), existing);
        }

        // 2) Lock both accounts in a fixed (alphabetical) order to prevent deadlocks.
        LockedPair accounts = lockInOrder(request.sourceIban(), request.targetIban());

        // 3) Re-check after acquiring the locks. A concurrent call with the same transferId
        //    may have committed while we were waiting. In READ COMMITTED each statement sees
        //    the latest committed data, so this query will find its postings.
        existing = postingRepository.findAllByTransferId(request.transferId());
        if (!existing.isEmpty()) {
            return PostingResponse.from(request.transferId(), existing);
        }

        // 4) Ownership: the initiator must own the source account.
        //    Same error as "not found" so we don't reveal that someone else's account exists.
        if (!accounts.source().getOwnerId().equals(request.initiatorId())) {
            throw new AccountNotFoundException(request.sourceIban());
        }

        // 5) Domain rules (active, currency, sufficient balance, valid amount) live in Account.
        //    If deposit fails after withdraw, the exception rolls back the whole transaction.
        accounts.source().withdraw(request.amount(), request.currency());
        accounts.target().deposit(request.amount(), request.currency());

        // 6) Double-entry: one DEBIT and one CREDIT with equal amounts.
        // saveAndFlush: run the INSERT here, so a UNIQUE violation surfaces as
        // DataIntegrityViolationException from this call (not later at commit time,
        // where it would be wrapped in a different exception type).
        Posting debit = postingRepository.saveAndFlush(Posting.debit(request.transferId(), accounts.source(), request.amount()));
        Posting credit = postingRepository.saveAndFlush(Posting.credit(request.transferId(), accounts.target(), request.amount()));

        return PostingResponse.from(request.transferId(), List.of(debit, credit));
    }

    /**
     * Locks the two accounts always in the same order (by IBAN), regardless of transfer direction.
     * Without this, A->B and B->A running concurrently could each hold one lock and wait
     * for the other forever (deadlock).
     */
    private LockedPair lockInOrder(String sourceIban, String targetIban) {
        boolean sourceFirst = sourceIban.compareTo(targetIban) < 0;
        String firstIban = sourceFirst ? sourceIban : targetIban;
        String secondIban = sourceFirst ? targetIban : sourceIban;

        Account first = accountRepository.findByAccountNumberForUpdate(firstIban)
                .orElseThrow(() -> new AccountNotFoundException(firstIban));
        Account second = accountRepository.findByAccountNumberForUpdate(secondIban)
                .orElseThrow(() -> new AccountNotFoundException(secondIban));

        return sourceFirst ? new LockedPair(first, second) : new LockedPair(second, first);
    }

    /** Returns the result of an already posted transfer, if any. Runs in its own read-only transaction. */
    @Transactional(readOnly = true)
    public Optional<PostingResponse> findPosted(Long transferId) {
        List<Posting> postings = postingRepository.findAllByTransferId(transferId);
        return postings.isEmpty()
                ? Optional.empty()
                : Optional.of(PostingResponse.from(transferId, postings));
    }

    private record LockedPair(Account source, Account target) {
    }
}
package com.gorkemuysal.minibank.transfer;

import com.gorkemuysal.minibank.transfer.client.AccountServiceClient;
import com.gorkemuysal.minibank.transfer.client.PostingOutcome;
import com.gorkemuysal.minibank.transfer.client.PostingRequest;
import com.gorkemuysal.minibank.transfer.common.RequestIdFilter;
import com.gorkemuysal.minibank.transfer.retry.RetryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

/**
 * Drives a single PENDING transfer towards a final state by calling account-service.
 * Used both by the POST request (first attempt) and by the retry job (later attempts).
 * <p>
 * Safe to call any number of times for the same transfer: account-service is idempotent
 * per transferId, so the money moves at most once no matter how often we retry.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransferProcessor {

    private final TransferRepository transferRepository;
    private final AccountServiceClient accountServiceClient;
    private final TransactionTemplate transactionTemplate;
    private final RetryProperties retry;

    /**
     * Makes one attempt and returns the transfer in its latest state.
     * Deliberately NOT @Transactional: the remote call must not hold a database connection
     * or pretend that a local rollback could undo what account-service already did.
     */
    public Transfer process(Long transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));

        // In the POST request the MDC already holds this ID. In the retry job it is empty,
        // so we restore the ID of the request that created the transfer: all attempts of
        // one transfer then log with the same ID, and it also travels to account-service.
        boolean boundHere = false;
        if (MDC.get(RequestIdFilter.MDC_KEY) == null && transfer.getRequestId() != null) {
            MDC.put(RequestIdFilter.MDC_KEY, transfer.getRequestId());
            boundHere = true;
        }
        try {
            return doProcess(transfer);
        } finally {
            // Only clean up what we set; never remove the ID of an enclosing HTTP request.
            if (boundHere) {
                MDC.remove(RequestIdFilter.MDC_KEY);
            }
        }
    }

    private Transfer doProcess(Transfer transfer) {
        Long transferId = transfer.getId();
        if (!transfer.isPending()) {
            return transfer;
        }

        PostingOutcome outcome = accountServiceClient.post(toPostingRequest(transfer));

        try {
            return transactionTemplate.execute(status -> applyOutcome(transferId, outcome));
        } catch (ObjectOptimisticLockingFailureException e) {
            log.info("Transfer {} was updated concurrently; keeping the other update", transferId);
            return transferRepository.findById(transferId).orElseThrow();
        }
    }

    private Transfer applyOutcome(Long transferId, PostingOutcome outcome) {
        // Re-read inside the transaction to get the latest state and version.
        Transfer transfer = transferRepository.findById(transferId).orElseThrow();
        if (!transfer.isPending()) {
            return transfer;
        }

        switch (outcome) {
            case PostingOutcome.Posted _ -> transfer.markCompleted();
            case PostingOutcome.Rejected rejected -> transfer.markFailed(rejected.errorCode());
            case PostingOutcome.Unknown unknown -> handleUnknown(transfer, unknown.reason());
        }
        return transfer;
    }

    /** Outcome unknown: schedule another attempt with backoff, or give up and ask a human. */
    private void handleUnknown(Transfer transfer, String reason) {
        int failedSoFar = transfer.getAttemptCount();

        if (failedSoFar + 1 >= retry.maxAttempts()) {
            transfer.markManualReview(reason);
            log.warn("Transfer {} moved to MANUAL_REVIEW after {} attempts. Last error: {}",
                    transfer.getId(), transfer.getAttemptCount(), reason);
            return;
        }

        Instant next = Instant.now().plus(retry.delayAfter(failedSoFar));
        transfer.scheduleRetry(reason, next);
        log.info("Transfer {} attempt {} failed ({}); next attempt at {}",
                transfer.getId(), transfer.getAttemptCount(), reason, next);
    }

    private static PostingRequest toPostingRequest(Transfer transfer) {
        return new PostingRequest(
                transfer.getId(),
                transfer.getInitiatorId(),
                transfer.getSourceIban(),
                transfer.getTargetIban(),
                transfer.getAmount(),
                transfer.getCurrency());
    }
}
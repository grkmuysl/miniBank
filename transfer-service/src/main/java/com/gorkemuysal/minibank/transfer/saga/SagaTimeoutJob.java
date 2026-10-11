package com.gorkemuysal.minibank.transfer.saga;

import com.gorkemuysal.minibank.transfer.Transfer;
import com.gorkemuysal.minibank.transfer.TransferRepository;
import com.gorkemuysal.minibank.transfer.TransferStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaTimeoutJob {

    private static final int BATCH_SIZE = 50;

    private final TransferRepository transferRepository;
    private final SagaCommands sagaCommands;
    private final SagaProperties sagaProperties;
    private final TransactionTemplate transactionTemplate;

    /**
     * Releases money held too long while waiting for a fraud decision.
     * Only FUNDS_HELD is timed out: it is before the pivot, so releasing is always safe.
     */
    @Scheduled(fixedDelay = 10_000, initialDelay = 15_000)
    public void releaseTimedOutHolds() {
        List<Long> ids = transferRepository.findIdsPastFraudDeadline(Instant.now(), PageRequest.of(0, BATCH_SIZE));
        for (Long id : ids) {
            try {
                transactionTemplate.executeWithoutResult(status -> timeOut(id));
            } catch (ObjectOptimisticLockingFailureException e) {
                // A fraud decision arrived for this transfer at the same moment. Whoever committed
                // first wins; if it was the decision, the transfer is no longer FUNDS_HELD next run.
                log.info("Transfer {} changed concurrently; skipping timeout", id);
            } catch (RuntimeException e) {
                log.error("Could not time out transfer {}", id, e);
            }
        }
    }

    private void timeOut(Long id) {
        Transfer transfer = transferRepository.findById(id).orElseThrow();
        // Re-check inside the transaction: the query result may already be stale.
        if (transfer.getStatus() != TransferStatus.FUNDS_HELD
                || transfer.getStateDeadlineAt().isAfter(Instant.now())) {
            return;
        }
        transfer.onFraudTimeout();
        sagaCommands.releaseFunds(transfer);
        log.warn("Transfer {}: no fraud decision within {}; releasing funds",
                id, sagaProperties.fraudTimeout());
    }

    /**
     * CAPTURING / RELEASING are NOT timed out. In CAPTURING the capture may already have happened
     * with its reply still on the way: releasing would try to undo a completed transfer. The
     * commands are safe in Kafka and will be processed once account-service is back.
     * We only make the situation visible (a metric and an alert in step 6).
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void warnAboutStuckTransfers() {
        Instant before = Instant.now().minus(sagaProperties.stuckThreshold());
        List<Long> stuck = transferRepository.findIdsInFlightSince(before, PageRequest.of(0, BATCH_SIZE));
        if (!stuck.isEmpty()) {
            log.warn("{} transfer(s) in CAPTURING/RELEASING for more than {}: {}",
                    stuck.size(), sagaProperties.stuckThreshold(), stuck);
        }
    }
}
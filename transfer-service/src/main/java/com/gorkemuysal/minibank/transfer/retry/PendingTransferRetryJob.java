package com.gorkemuysal.minibank.transfer.retry;

import com.gorkemuysal.minibank.transfer.TransferProcessor;
import com.gorkemuysal.minibank.transfer.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Periodically retries PENDING transfers whose next attempt is due.
 * Known limitation: with more than one transfer-service instance, every instance runs this job
 * and may pick the same transfers. That is safe for money (account-service is idempotent and
 * the Transfer @Version resolves concurrent updates) but wasteful. To be solved with ShedLock.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingTransferRetryJob {

    private static final int BATCH_SIZE = 20;

    private final TransferRepository transferRepository;
    private final TransferProcessor transferProcessor;

    // fixedDelay: the next run starts 5s after the previous one FINISHES, so slow runs
    // (e.g. account-service timing out on every call) never overlap.
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    public void retryDueTransfers() {
        List<Long> dueIds = transferRepository.findIdsDueForRetry(Instant.now(), PageRequest.of(0, BATCH_SIZE));
        if (dueIds.isEmpty()) {
            return;
        }

        log.info("Retrying {} pending transfer(s)", dueIds.size());
        for (Long id : dueIds) {
            try {
                transferProcessor.process(id);
            } catch (RuntimeException e) {
                // One broken transfer must not block the rest of the batch.
                log.error("Unexpected error while retrying transfer {}", id, e);
            }
        }
    }
}
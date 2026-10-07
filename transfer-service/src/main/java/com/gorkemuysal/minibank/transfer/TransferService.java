package com.gorkemuysal.minibank.transfer;

import com.gorkemuysal.minibank.transfer.dto.CreateTransferRequest;
import com.gorkemuysal.minibank.transfer.exception.BusinessException;
import com.gorkemuysal.minibank.transfer.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRepository transferRepository;
    private final TransferProcessor transferProcessor;

    /**
     * Creates a PENDING transfer in an idempotent way.
     * <p>
     * The same (initiatorId, idempotencyKey) pair always results in a single transfer:
     * <ul>
     *   <li>Key not seen before: a new transfer is created (created = true, HTTP 201).</li>
     *   <li>Key seen with the same request: the existing transfer is returned (created = false, HTTP 200).</li>
     *   <li>Key seen with a different request: IDEMPOTENCY_KEY_REUSED (HTTP 422).</li>
     * </ul>
     * <p>
     * This method is intentionally NOT {@code @Transactional}. If the whole method ran in a single
     * transaction, a unique constraint violation would put the PostgreSQL transaction into an
     * "aborted" state, so the re-read in the catch block would fail with
     * "current transaction is aborted". Spring would also mark the transaction as rollback-only,
     * causing an UnexpectedRollbackException even if we caught the exception.
     * Without an outer transaction, each repository call runs in its own short transaction,
     * so a failed insert does not affect the subsequent read.
     */
    public CreateTransferResult create(String initiatorId, String idempotencyKey, CreateTransferRequest request) {
        // A transfer to the same account makes no sense; reject it before touching the database.
        if (request.sourceIban().equals(request.targetIban())) {
            throw new BusinessException(ErrorCode.SAME_IBAN_TRANSFER);
        }

        // 1) Fast path: has this key been used by this initiator before?
        //    This check is only an optimization; it cannot prevent concurrent duplicates on its own.
        Optional<Transfer> existing = transferRepository.findByInitiatorIdAndIdempotencyKey(initiatorId, idempotencyKey);
        if (existing.isPresent()) {
            return replay(existing.get(), request);
        }

        // 2) Insert a new transfer. The real guarantee is the UNIQUE (initiator_id, idempotency_key)
        //    constraint in the database. saveAndFlush (instead of save) forces the INSERT to run
        //    right here, so a constraint violation is thrown inside this try block.
        try {
            Transfer saved = transferRepository.saveAndFlush(
                    Transfer.createPending(initiatorId, idempotencyKey, request));
            Transfer processed = transferProcessor.process(saved.getId());
            return new CreateTransferResult(processed, true);
        } catch (DataIntegrityViolationException e) {
            // 3) We lost the race: a concurrent request with the same key inserted first.
            //    PostgreSQL blocks our INSERT until the other transaction commits, so by the time
            //    we get here the winning row is committed and visible to this read.
            Transfer winner = transferRepository.findByInitiatorIdAndIdempotencyKey(initiatorId, idempotencyKey)
                    // No row found means the violation came from a different constraint; rethrow it as is.
                    .orElseThrow(() -> e);
            return replay(winner, request);
        }
    }

    /**
     * Handles a repeated request for an existing idempotency key.
     * Returns the existing transfer if the request describes the same operation;
     * otherwise rejects it, because a key must never be reused for a different transfer.
     */
    private CreateTransferResult replay(Transfer existing, CreateTransferRequest request) {
        if (!existing.matches(request)) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
        }
        return new CreateTransferResult(existing, false);
    }

    /**
     * Returns a transfer only if it belongs to the given initiator.
     * Transfers of other users result in 404 (not 403), so their existence is not revealed.
     */
    @Transactional(readOnly = true)
    public Transfer getById(Long id, String initiatorId) {
        return transferRepository.findByIdAndInitiatorId(id, initiatorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSFER_NOT_FOUND));
    }

    /**
     * Lists the initiator's transfers, newest first.
     * The query can use the index created by the UNIQUE (initiator_id, idempotency_key) constraint,
     * since initiator_id is its leading column.
     */
    @Transactional(readOnly = true)
    public List<Transfer> listForInitiator(String initiatorId) {
        return transferRepository.findAllByInitiatorIdOrderByCreatedAtDesc(initiatorId);
    }
}
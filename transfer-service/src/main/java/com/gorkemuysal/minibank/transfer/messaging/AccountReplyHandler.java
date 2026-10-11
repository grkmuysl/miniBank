package com.gorkemuysal.minibank.transfer.messaging;

import com.gorkemuysal.minibank.transfer.Transfer;
import com.gorkemuysal.minibank.transfer.TransferRepository;
import com.gorkemuysal.minibank.transfer.TransferStatus;
import com.gorkemuysal.minibank.transfer.messaging.payload.CaptureFailedPayload;
import com.gorkemuysal.minibank.transfer.saga.SagaCommands;
import com.gorkemuysal.minibank.transfer.saga.SagaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

/**
 * Applies account-service's replies. Pattern, in one transaction:
 * inbox check -> is this reply expected in the current state? -> state change -> next command (outbox).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountReplyHandler {

    private final Inbox inbox;
    private final TransferRepository transferRepository;
    private final SagaCommands sagaCommands;
    private final SagaProperties sagaProperties;
    private final JsonMapper jsonMapper;

    @Transactional
    public void handle(MessageEnvelope message) {
        if (!inbox.markProcessed(message.messageId())) {
            log.info("Duplicate message {} ({}) for transfer {} ignored",
                    message.messageId(), message.type(), message.transferId());
            return;
        }
        Transfer transfer = transferRepository.findById(message.transferId()).orElse(null);
        if (transfer == null) {
            log.warn("Reply {} for unknown transfer {} ignored", message.type(), message.transferId());
            return;
        }

        switch (message.type()) {
            case MessageTypes.FUNDS_HELD -> onFundsHeld(transfer, message);
            case MessageTypes.HOLD_FAILED -> {
                if (expects(transfer, TransferStatus.PENDING, message)) {
                    String code = jsonMapper.treeToValue(message.payload(), HoldFailedPayload.class).code();
                    transfer.onHoldFailed(code);
                    log.info("Transfer {} failed: {}", transfer.getId(), code);
                }
            }
            case MessageTypes.FUNDS_CAPTURED -> {
                if (expects(transfer, TransferStatus.CAPTURING, message)) {
                    transfer.onCaptured();
                    log.info("Transfer {} completed", transfer.getId());
                }
            }
            case MessageTypes.CAPTURE_FAILED -> {
                if (expects(transfer, TransferStatus.CAPTURING, message)) {
                    String code = jsonMapper.treeToValue(message.payload(), CaptureFailedPayload.class).code();
                    transfer.onCaptureFailed(code);
                    sagaCommands.releaseFunds(transfer);
                    log.warn("Transfer {} capture failed ({}); releasing funds", transfer.getId(), code);
                }
            }
            case MessageTypes.FUNDS_RELEASED -> {
                if (expects(transfer, TransferStatus.RELEASING, message)) {
                    transfer.onReleased();
                    log.info("Transfer {} rejected ({}), funds released", transfer.getId(), transfer.getReleaseReason());
                }
            }
            default -> throw new IllegalArgumentException("Unknown reply type: " + message.type());
        }
    }

    private void onFundsHeld(Transfer transfer, MessageEnvelope message) {
        if (transfer.getStatus() == TransferStatus.FAILED) {
            // A hold exists for a transfer we already gave up on (see 5.2). Nobody would ever
            // release it, so release it now. Harmless if no hold actually exists.
            log.warn("FUNDS_HELD for FAILED transfer {}: releasing the orphan hold", transfer.getId());
            sagaCommands.releaseFunds(transfer);
            return;
        }
        if (expects(transfer, TransferStatus.PENDING, message)) {
            transfer.onFundsHeld(Instant.now().plus(sagaProperties.fraudTimeout()));
            sagaCommands.requestFraudCheck(transfer);
            log.info("Transfer {} funds held, fraud check requested", transfer.getId());
        }
    }

    /** Late or duplicated messages must never move a transfer backwards or sideways. */
    private boolean expects(Transfer transfer, TransferStatus expected, MessageEnvelope message) {
        if (transfer.getStatus() == expected) {
            return true;
        }
        log.info("{} for transfer {} ignored: expected {}, transfer is {}",
                message.type(), transfer.getId(), expected, transfer.getStatus());
        return false;
    }
}
package com.gorkemuysal.minibank.transfer.messaging;

import com.gorkemuysal.minibank.transfer.Transfer;
import com.gorkemuysal.minibank.transfer.TransferRepository;
import com.gorkemuysal.minibank.transfer.messaging.payload.FraudCheckRequestedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Applies account-service's replies to the transfer, in one transaction with the inbox record.
 * <p>
 * A reply only moves a transfer forward from the state that expects it (PENDING).
 * Late or duplicated replies must never move a transfer backwards.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountReplyHandler {

    private final Inbox inbox;
    private final TransferRepository transferRepository;
    private final JsonMapper jsonMapper;

    private final OutboxWriter outboxWriter;

    @Transactional
    public void handle(MessageEnvelope message) {
        if (!inbox.markProcessed(message.messageId())) {
            log.info("Duplicate message {} ({}) for transfer {} ignored",
                    message.messageId(), message.type(), message.transferId());
            return;
        }

        Transfer transfer = transferRepository.findById(message.transferId()).orElse(null);
        if (transfer == null) {
            // Not ours (e.g. a reply to a manually produced test command). Retrying won't help.
            log.warn("Reply {} for unknown transfer {} ignored", message.type(), message.transferId());
            return;
        }
        if (!transfer.isPending()) {
            log.info("Reply {} for transfer {} ignored: transfer is already {}",
                    message.type(), transfer.getId(), transfer.getStatus());
            return;
        }

        switch (message.type()) {
            case MessageTypes.FUNDS_HELD -> {
                transfer.markFundsHeld();
                // Next saga step, in the same transaction as the state change (outbox).
                // Keyed by customer: all of one customer's checks are processed in order.
                outboxWriter.writeWithKey(FraudTopics.CHECK_REQUESTS, transfer.getInitiatorId(),
                        MessageTypes.FRAUD_CHECK_REQUESTED, transfer.getId(), transfer.getRequestId(),
                        FraudCheckRequestedPayload.from(transfer));
                log.info("Transfer {} funds held, fraud check requested", transfer.getId());
            }
            case MessageTypes.HOLD_FAILED -> {
                HoldFailedPayload p = jsonMapper.treeToValue(message.payload(), HoldFailedPayload.class);
                transfer.markFailed(p.code());
                log.info("Transfer {} failed: {}", transfer.getId(), p.code());
            }
            default -> throw new IllegalArgumentException("Unknown reply type: " + message.type());
        }
    }
}
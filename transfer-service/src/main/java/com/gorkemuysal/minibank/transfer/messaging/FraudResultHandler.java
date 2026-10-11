package com.gorkemuysal.minibank.transfer.messaging;

import com.gorkemuysal.minibank.transfer.Transfer;
import com.gorkemuysal.minibank.transfer.TransferRepository;
import com.gorkemuysal.minibank.transfer.TransferStatus;
import com.gorkemuysal.minibank.transfer.messaging.payload.FraudResultPayload;
import com.gorkemuysal.minibank.transfer.saga.SagaCommands;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Applies fraud-service's decision. Only a FUNDS_HELD transfer accepts it. */
@Slf4j
@Component
@RequiredArgsConstructor
public class FraudResultHandler {

    private final Inbox inbox;
    private final TransferRepository transferRepository;
    private final JsonMapper jsonMapper;
    private final SagaCommands sagaCommands;

    @Transactional
    public void handle(MessageEnvelope message) {
        if (!inbox.markProcessed(message.messageId())) {
            log.info("Duplicate message {} ({}) for transfer {} ignored",
                    message.messageId(), message.type(), message.transferId());
            return;
        }

        Transfer transfer = transferRepository.findById(message.transferId()).orElse(null);
        if (transfer == null) {
            log.warn("Fraud result {} for unknown transfer {} ignored", message.type(), message.transferId());
            return;
        }
        if (transfer.getStatus() != TransferStatus.FUNDS_HELD) {
            // Duplicates (fraud-service re-publishes stored decisions) and late decisions
            // (after a FRAUD_TIMEOUT) end up here. A late approval must NOT capture.
            log.info("Fraud result {} for transfer {} ignored: transfer is already {}",
                    message.type(), transfer.getId(), transfer.getStatus());
            return;
        }

        switch (message.type()) {
            case MessageTypes.FRAUD_APPROVED -> {
                transfer.onFraudApproved();
                sagaCommands.captureFunds(transfer);
                log.info("Transfer {} approved by fraud check, capture requested", transfer.getId());
            }
            case MessageTypes.FRAUD_REJECTED -> {
                FraudResultPayload p = jsonMapper.treeToValue(message.payload(), FraudResultPayload.class);
                String reasons = String.join(",", p.reasons());
                transfer.onFraudRejected(reasons);
                sagaCommands.releaseFunds(transfer);
                log.info("Transfer {} rejected by fraud check ({}), release requested", transfer.getId(), reasons);
            }
            default -> throw new IllegalArgumentException("Unknown fraud result type: " + message.type());
        }
    }
}
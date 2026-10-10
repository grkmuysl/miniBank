package com.gorkemuysal.minibank.messaging;

import com.gorkemuysal.minibank.hold.HoldResult;
import com.gorkemuysal.minibank.hold.HoldService;
import com.gorkemuysal.minibank.hold.dto.PlaceHoldRequest;
import com.gorkemuysal.minibank.messaging.inbox.Inbox;
import com.gorkemuysal.minibank.messaging.outbox.OutboxWriter;
import com.gorkemuysal.minibank.messaging.payload.FundsHeldPayload;
import com.gorkemuysal.minibank.messaging.payload.HoldFailedPayload;
import com.gorkemuysal.minibank.messaging.payload.HoldFundsPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Processes one command in ONE transaction: inbox record + business change + outbox reply.
 * Either all three are committed, or none (and Kafka redelivers the message).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountCommandHandler {

    private final Inbox inbox;
    private final HoldService holdService;
    private final OutboxWriter outboxWriter;
    private final JsonMapper jsonMapper;

    @Transactional
    public void handle(MessageEnvelope message) {
        if (!inbox.markProcessed(message.messageId())) {
            log.info("Duplicate message {} ({}) for transfer {} ignored",
                    message.messageId(), message.type(), message.transferId());
            return;
        }

        switch (message.type()) {
            case MessageTypes.HOLD_FUNDS -> handleHoldFunds(message);
            // An unknown type is a contract/programming error, not a business outcome.
            // Throwing lets Spring Kafka retry and then give up (DLT in 5.5).
            default -> throw new IllegalArgumentException("Unknown command type: " + message.type());
        }
    }

    private void handleHoldFunds(MessageEnvelope message) {
        HoldFundsPayload p = jsonMapper.treeToValue(message.payload(), HoldFundsPayload.class);
        var request = new PlaceHoldRequest(message.transferId(), p.initiatorId(),
                p.sourceIban(), p.targetIban(), p.amount(), p.currency());

        switch (holdService.tryPlaceHold(request)) {
            case HoldResult.Held held -> outboxWriter.write(AccountTopics.REPLIES, MessageTypes.FUNDS_HELD,
                    message.transferId(), message.requestId(), new FundsHeldPayload(held.amount()));
            case HoldResult.Rejected rejected -> outboxWriter.write(AccountTopics.REPLIES, MessageTypes.HOLD_FAILED,
                    message.transferId(), message.requestId(), new HoldFailedPayload(rejected.code(), rejected.message()));
        }
    }
}
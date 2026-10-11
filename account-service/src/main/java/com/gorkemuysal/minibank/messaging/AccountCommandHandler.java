package com.gorkemuysal.minibank.messaging;

import com.gorkemuysal.minibank.hold.CaptureResult;
import com.gorkemuysal.minibank.hold.HoldResult;
import com.gorkemuysal.minibank.hold.HoldService;
import com.gorkemuysal.minibank.hold.dto.PlaceHoldRequest;
import com.gorkemuysal.minibank.messaging.inbox.Inbox;
import com.gorkemuysal.minibank.messaging.outbox.OutboxWriter;
import com.gorkemuysal.minibank.messaging.payload.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

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
    private final MessageValidator messageValidator;

    @Transactional
    public void handle(MessageEnvelope message) {
        if (!inbox.markProcessed(message.messageId())) {
            log.info("Duplicate message {} ({}) for transfer {} ignored",
                    message.messageId(), message.type(), message.transferId());
            return;
        }

        switch (message.type()) {
            case MessageTypes.HOLD_FUNDS -> handleHoldFunds(message);
            case MessageTypes.CAPTURE_FUNDS -> handleCaptureFunds(message);
            case MessageTypes.RELEASE_FUNDS -> handleReleaseFunds(message);
            default -> throw new MalformedMessageException("Unknown message type: " + message.type());
        }
    }

    private void handleHoldFunds(MessageEnvelope message) {
        HoldFundsPayload p = payload(message, HoldFundsPayload.class);
        var request = messageValidator.requireValid(
                new PlaceHoldRequest(message.transferId(), p.initiatorId(),
                        p.sourceIban(), p.targetIban(), p.amount(), p.currency()),
                message.type());

        switch (holdService.tryPlaceHold(request)) {
            case HoldResult.Held held -> outboxWriter.write(AccountTopics.REPLIES, MessageTypes.FUNDS_HELD,
                    message.transferId(), message.requestId(), new FundsHeldPayload(held.amount()));
            case HoldResult.Rejected rejected -> outboxWriter.write(AccountTopics.REPLIES, MessageTypes.HOLD_FAILED,
                    message.transferId(), message.requestId(), new HoldFailedPayload(rejected.code(), rejected.message()));
        }
    }

    private void handleCaptureFunds(MessageEnvelope message) {
        switch (holdService.tryCapture(message.transferId())) {
            case CaptureResult.Captured captured -> outboxWriter.write(AccountTopics.REPLIES,
                    MessageTypes.FUNDS_CAPTURED, message.transferId(), message.requestId(),
                    new FundsCapturedPayload(captured.amount()));
            case CaptureResult.Rejected rejected -> outboxWriter.write(AccountTopics.REPLIES,
                    MessageTypes.CAPTURE_FAILED, message.transferId(), message.requestId(),
                    new CaptureFailedPayload(rejected.code(), rejected.message()));
        }
    }

    private void handleReleaseFunds(MessageEnvelope message) {
        // Either succeeds or throws a technical error (rolls back, retried, DLT in 5.5).
        holdService.releaseForCompensation(message.transferId());
        outboxWriter.write(AccountTopics.REPLIES, MessageTypes.FUNDS_RELEASED,
                message.transferId(), message.requestId(), Map.of());
    }

    private <T> T payload(MessageEnvelope message, Class<T> type) {
        if (message.payload() == null || message.payload().isNull()) {
            throw new MalformedMessageException(message.type() + " has no payload");
        }
        try {
            return jsonMapper.treeToValue(message.payload(), type);
        } catch (JacksonException e) {
            throw new MalformedMessageException("Invalid " + message.type() + " payload: " + e.getOriginalMessage(), e);
        }
    }
}
package com.gorkemuysal.minibank.fraud.messaging;

import com.gorkemuysal.minibank.fraud.check.Decision;
import com.gorkemuysal.minibank.fraud.check.FraudCheck;
import com.gorkemuysal.minibank.fraud.check.FraudCheckContext;
import com.gorkemuysal.minibank.fraud.check.FraudCheckService;
import com.gorkemuysal.minibank.fraud.messaging.payload.FraudCheckRequestedPayload;
import com.gorkemuysal.minibank.fraud.messaging.payload.FraudResultPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Inbox + decision + outbox result, in one transaction. */
@Slf4j
@Component
@RequiredArgsConstructor
public class FraudRequestHandler {

    private final Inbox inbox;
    private final FraudCheckService fraudCheckService;
    private final OutboxWriter outboxWriter;
    private final JsonMapper jsonMapper;

    @Transactional
    public void handle(MessageEnvelope message) {
        if (!inbox.markProcessed(message.messageId())) {
            log.info("Duplicate message {} for transfer {} ignored", message.messageId(), message.transferId());
            return;
        }
        if (!MessageTypes.FRAUD_CHECK_REQUESTED.equals(message.type())) {
            throw new IllegalArgumentException("Unknown message type: " + message.type());
        }

        FraudCheckRequestedPayload p = jsonMapper.treeToValue(message.payload(), FraudCheckRequestedPayload.class);
        FraudCheck check = fraudCheckService.check(new FraudCheckContext(
                message.transferId(), p.initiatorId(), p.sourceIban(), p.targetIban(),
                p.amount(), p.currency(), p.requestedAt()));

        // Also for a stored decision: re-publishing it lets a requester that missed the first
        // result (or sent a duplicate request) still get the same, unchanged answer.
        String type = check.getDecision() == Decision.APPROVED
                ? MessageTypes.FRAUD_APPROVED
                : MessageTypes.FRAUD_REJECTED;
        outboxWriter.write(FraudTopics.RESULTS, type, message.transferId(), message.requestId(),
                new FraudResultPayload(check.reasonList()));
    }
}
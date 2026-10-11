package com.gorkemuysal.minibank.transfer.saga;

import com.gorkemuysal.minibank.transfer.Transfer;
import com.gorkemuysal.minibank.transfer.messaging.*;
import com.gorkemuysal.minibank.transfer.messaging.payload.FraudCheckRequestedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Every command the saga can send, in one place. Each one is written to the outbox, so it must be
 * called inside the transaction that changes the transfer's state.
 */
@Component
@RequiredArgsConstructor
public class SagaCommands {

    private final OutboxWriter outboxWriter;

    public void holdFunds(Transfer t) {
        outboxWriter.write(AccountTopics.COMMANDS, MessageTypes.HOLD_FUNDS,
                t.getId(), t.getRequestId(), HoldFundsPayload.from(t));
    }

    /** Keyed by customer: the velocity rule needs one customer's checks processed in order. */
    public void requestFraudCheck(Transfer t) {
        outboxWriter.writeWithKey(FraudTopics.CHECK_REQUESTS, t.getInitiatorId(),
                MessageTypes.FRAUD_CHECK_REQUESTED, t.getId(), t.getRequestId(),
                FraudCheckRequestedPayload.from(t));
    }

    public void captureFunds(Transfer t) {
        outboxWriter.write(AccountTopics.COMMANDS, MessageTypes.CAPTURE_FUNDS,
                t.getId(), t.getRequestId(), Map.of());
    }

    public void releaseFunds(Transfer t) {
        outboxWriter.write(AccountTopics.COMMANDS, MessageTypes.RELEASE_FUNDS,
                t.getId(), t.getRequestId(), Map.of());
    }
}
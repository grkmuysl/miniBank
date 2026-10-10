package com.gorkemuysal.minibank.messaging;

/** Message types on account.commands / account.replies. This is account-service's async API. */
public final class MessageTypes {

    // Commands (account.commands)
    public static final String HOLD_FUNDS = "HOLD_FUNDS";

    // Replies (account.replies)
    public static final String FUNDS_HELD = "FUNDS_HELD";
    public static final String HOLD_FAILED = "HOLD_FAILED";

    private MessageTypes() {
    }
}
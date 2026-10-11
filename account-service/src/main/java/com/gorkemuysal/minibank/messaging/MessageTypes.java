package com.gorkemuysal.minibank.messaging;

/** Message types on account.commands / account.replies. This is account-service's async API. */
public final class MessageTypes {

    // Commands (account.commands)
    public static final String HOLD_FUNDS = "HOLD_FUNDS";
    public static final String CAPTURE_FUNDS = "CAPTURE_FUNDS";
    public static final String RELEASE_FUNDS = "RELEASE_FUNDS";

    // Replies (account.replies)
    public static final String FUNDS_HELD = "FUNDS_HELD";
    public static final String HOLD_FAILED = "HOLD_FAILED";
    public static final String FUNDS_CAPTURED = "FUNDS_CAPTURED";
    public static final String CAPTURE_FAILED = "CAPTURE_FAILED";
    public static final String FUNDS_RELEASED = "FUNDS_RELEASED";

    private MessageTypes() {
    }
}
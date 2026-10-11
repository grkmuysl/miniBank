package com.gorkemuysal.minibank.transfer.messaging;

public final class MessageTypes {

    public static final String HOLD_FUNDS = "HOLD_FUNDS";
    public static final String FUNDS_HELD = "FUNDS_HELD";
    public static final String HOLD_FAILED = "HOLD_FAILED";

    public static final String FRAUD_CHECK_REQUESTED = "FRAUD_CHECK_REQUESTED";
    public static final String FRAUD_APPROVED = "FRAUD_APPROVED";
    public static final String FRAUD_REJECTED = "FRAUD_REJECTED";

    public static final String CAPTURE_FUNDS = "CAPTURE_FUNDS";
    public static final String RELEASE_FUNDS = "RELEASE_FUNDS";
    public static final String FUNDS_CAPTURED = "FUNDS_CAPTURED";
    public static final String CAPTURE_FAILED = "CAPTURE_FAILED";
    public static final String FUNDS_RELEASED = "FUNDS_RELEASED";

    private MessageTypes() {
    }
}
package com.gorkemuysal.minibank.fraud.messaging;

public final class MessageTypes {

    public static final String FRAUD_CHECK_REQUESTED = "FRAUD_CHECK_REQUESTED";
    public static final String FRAUD_APPROVED = "FRAUD_APPROVED";
    public static final String FRAUD_REJECTED = "FRAUD_REJECTED";

    private MessageTypes() {
    }
}
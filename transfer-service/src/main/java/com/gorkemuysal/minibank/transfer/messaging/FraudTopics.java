package com.gorkemuysal.minibank.transfer.messaging;

/** Names of fraud-service's topics. fraud-service owns and creates them. */
public final class FraudTopics {

    public static final String CHECK_REQUESTS = "fraud.check-requests";
    public static final String RESULTS = "fraud.results";

    private FraudTopics() {
    }
}
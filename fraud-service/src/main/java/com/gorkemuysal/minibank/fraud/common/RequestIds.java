package com.gorkemuysal.minibank.fraud.common;

import java.util.regex.Pattern;

/** Same request ID rule as the HTTP services (4.3): short, harmless characters only. */
public final class RequestIds {

    public static final String MDC_KEY = "requestId";

    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9-]{1,64}$");

    public static boolean isValid(String requestId) {
        return requestId != null && VALID.matcher(requestId).matches();
    }

    private RequestIds() {
    }
}
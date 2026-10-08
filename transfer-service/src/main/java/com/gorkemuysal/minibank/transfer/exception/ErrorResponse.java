package com.gorkemuysal.minibank.transfer.exception;

import com.gorkemuysal.minibank.transfer.common.RequestIdFilter;
import org.slf4j.MDC;

import java.time.Instant;

public record ErrorResponse(String code, String message, String requestId, Instant timestamp) {

    public static ErrorResponse of(ErrorCode code, String message) {
        return new ErrorResponse(code.name(), message, MDC.get(RequestIdFilter.MDC_KEY), Instant.now());
    }
}
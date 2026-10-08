package com.gorkemuysal.minibank.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String error,
        String code,
        String message,
        List<FieldViolation> fieldErrors,
        String requestId
) {
    public record FieldViolation(String field, String message) {}
}
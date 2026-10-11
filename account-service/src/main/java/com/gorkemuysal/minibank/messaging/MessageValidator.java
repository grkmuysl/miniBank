package com.gorkemuysal.minibank.messaging;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bean Validation for message payloads. @Valid only works on controller parameters;
 * on the messaging path we have to call the validator ourselves.
 * An invalid payload is a broken contract between services, not a customer error: DLT.
 */
@Component
@RequiredArgsConstructor
public class MessageValidator {

    private final Validator validator;

    public <T> T requireValid(T payload, String messageType) {
        Set<ConstraintViolation<T>> violations = validator.validate(payload);
        if (!violations.isEmpty()) {
            String details = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new MalformedMessageException(messageType + " payload is invalid: " + details);
        }
        return payload;
    }
}
package com.gorkemuysal.minibank.transfer.exception;

/**
 * The system reached a state that must be impossible, e.g. releasing a CAPTURED hold
 * or a refused state transition. Always a bug. Never retried, never swallowed:
 * the message goes to the DLT and a human must look at it.
 * <p>
 * Deliberately NOT a BusinessException: the messaging-path "try..." methods catch
 * BusinessException and turn it into a reply. An invariant violation must escape them.
 */
public class SagaInvariantViolationException extends RuntimeException {

    public SagaInvariantViolationException(String message) {
        super(message);
    }

    public SagaInvariantViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
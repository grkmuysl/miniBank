package com.gorkemuysal.minibank.fraud.rules;

import com.gorkemuysal.minibank.fraud.check.FraudCheckContext;

import java.util.Optional;

/**
 * One fraud rule (Strategy pattern). Returns its code if triggered, empty otherwise.
 * Adding a rule = adding a new @Component implementing this; no existing code changes.
 */
public interface FraudRule {

    Optional<String> evaluate(FraudCheckContext context);
}
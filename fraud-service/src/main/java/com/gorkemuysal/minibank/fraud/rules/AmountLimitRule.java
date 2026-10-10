package com.gorkemuysal.minibank.fraud.rules;

import com.gorkemuysal.minibank.fraud.check.FraudCheckContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AmountLimitRule implements FraudRule {

    static final String CODE = "AMOUNT_LIMIT_EXCEEDED";

    private final FraudProperties properties;

    @Override
    public Optional<String> evaluate(FraudCheckContext ctx) {
        boolean exceeded = properties.amountLimitFor(ctx.currency())
                .map(limit -> ctx.amount().compareTo(limit) > 0)
                .orElse(true);   // no limit configured for this currency: fail closed
        return exceeded ? Optional.of(CODE) : Optional.empty();
    }
}
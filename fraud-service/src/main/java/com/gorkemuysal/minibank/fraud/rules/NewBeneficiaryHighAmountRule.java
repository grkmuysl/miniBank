package com.gorkemuysal.minibank.fraud.rules;

import com.gorkemuysal.minibank.fraud.check.FraudCheckContext;
import com.gorkemuysal.minibank.fraud.check.FraudCheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** A large amount to someone this customer has never successfully sent money to before. */
@Component
@RequiredArgsConstructor
public class NewBeneficiaryHighAmountRule implements FraudRule {

    static final String CODE = "NEW_BENEFICIARY_HIGH_AMOUNT";

    private final FraudCheckRepository fraudCheckRepository;
    private final FraudProperties properties;

    @Override
    public Optional<String> evaluate(FraudCheckContext ctx) {
        boolean highAmount = properties.newBeneficiaryLimitFor(ctx.currency())
                .map(limit -> ctx.amount().compareTo(limit) > 0)
                .orElse(true);   // fail closed
        if (!highAmount) {
            return Optional.empty();   // cheap check first; skip the query for small amounts
        }
        boolean knownBeneficiary = fraudCheckRepository.hasApprovedTransferTo(ctx.initiatorId(), ctx.targetIban());
        return knownBeneficiary ? Optional.empty() : Optional.of(CODE);
    }
}
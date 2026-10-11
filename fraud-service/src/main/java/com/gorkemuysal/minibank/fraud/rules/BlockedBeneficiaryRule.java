package com.gorkemuysal.minibank.fraud.rules;

import com.gorkemuysal.minibank.fraud.check.BlockedIbanRepository;
import com.gorkemuysal.minibank.fraud.check.FraudCheckContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BlockedBeneficiaryRule implements FraudRule {

    static final String CODE = "BLOCKED_BENEFICIARY";

    private final BlockedIbanRepository blockedIbanRepository;

    @Override
    public Optional<String> evaluate(FraudCheckContext ctx) {
        return blockedIbanRepository.existsById(ctx.targetIban()) ? Optional.of(CODE) : Optional.empty();
    }
}
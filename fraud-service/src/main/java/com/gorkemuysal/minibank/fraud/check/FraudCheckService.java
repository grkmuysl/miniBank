package com.gorkemuysal.minibank.fraud.check;

import com.gorkemuysal.minibank.fraud.rules.FraudRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudCheckService {

    private final FraudCheckRepository fraudCheckRepository;
    private final List<FraudRule> rules;

    /**
     * Returns the decision for this transfer, computing it only the first time.
     * <p>
     * A decision is NEVER recomputed: by the time a duplicate request arrives the customer's
     * history has changed (e.g. more transfers in the velocity window), so re-running the rules
     * could produce a different answer and the saga would receive two contradictory decisions.
     */
    @Transactional
    public FraudCheck check(FraudCheckContext ctx) {
        Optional<FraudCheck> existing = fraudCheckRepository.findByTransferId(ctx.transferId());
        if (existing.isPresent()) {
            log.info("Transfer {} already decided ({}); returning stored decision",
                    ctx.transferId(), existing.get().getDecision());
            return existing.get();
        }

        // Run ALL rules; don't stop at the first hit. A rejection must list every reason.
        List<String> triggered = rules.stream()
                .map(rule -> rule.evaluate(ctx))
                .flatMap(Optional::stream)
                .sorted()   // stable order in the stored reasons, independent of bean order
                .toList();

        FraudCheck check = fraudCheckRepository.save(FraudCheck.decide(ctx, triggered));
        log.info("Transfer {} {} {}", ctx.transferId(), check.getDecision(),
                triggered.isEmpty() ? "" : triggered);
        return check;
    }
}
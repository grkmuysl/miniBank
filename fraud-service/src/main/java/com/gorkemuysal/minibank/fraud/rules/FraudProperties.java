package com.gorkemuysal.minibank.fraud.rules;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ConfigurationProperties("fraud")
public record FraudProperties(
        Map<String, BigDecimal> amountLimit,
        Map<String, BigDecimal> newBeneficiaryLimit,
        Velocity velocity
) {
    public record Velocity(int maxChecks, Duration window) {
    }

    public FraudProperties {
        // Normalize currency keys, so "try" in an environment variable still matches "TRY".
        amountLimit = upperCaseKeys(amountLimit);
        newBeneficiaryLimit = upperCaseKeys(newBeneficiaryLimit);
    }

    /** Empty if no limit is configured for this currency; rules then fail closed. */
    public Optional<BigDecimal> amountLimitFor(String currency) {
        return Optional.ofNullable(amountLimit.get(currency.toUpperCase(Locale.ROOT)));
    }

    public Optional<BigDecimal> newBeneficiaryLimitFor(String currency) {
        return Optional.ofNullable(newBeneficiaryLimit.get(currency.toUpperCase(Locale.ROOT)));
    }

    private static Map<String, BigDecimal> upperCaseKeys(Map<String, BigDecimal> map) {
        return map == null ? Map.of() : map.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        e -> e.getKey().toUpperCase(Locale.ROOT), Map.Entry::getValue));
    }
}
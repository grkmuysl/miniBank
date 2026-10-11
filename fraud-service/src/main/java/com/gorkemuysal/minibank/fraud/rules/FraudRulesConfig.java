package com.gorkemuysal.minibank.fraud.rules;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FraudProperties.class)
public class FraudRulesConfig {
}
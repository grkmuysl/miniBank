package com.gorkemuysal.minibank.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Coarse-grained check at the edge: rejects requests without a valid token early,
 * so they never reach the services. Fine-grained authorization (roles, audience,
 * ownership) stays in each service. Services MUST keep validating tokens themselves
 * (zero trust: the internal network is not trusted either).
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Token-based API without cookies: CSRF protection does not apply.
                .csrf(csrf -> csrf.disable())
                .build();
    }
}
package com.gorkemuysal.minibank.gateway;

import com.gorkemuysal.minibank.gateway.ratelimit.BucketStore;
import com.gorkemuysal.minibank.gateway.ratelimit.RateLimitResponder;
import com.gorkemuysal.minibank.gateway.ratelimit.UserRateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            BucketStore bucketStore,
                                            RateLimitResponder rateLimitResponder) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                // Created with "new" on purpose; see UserRateLimitFilter's Javadoc.
                .addFilterAfter(new UserRateLimitFilter(bucketStore, rateLimitResponder),
                        BearerTokenAuthenticationFilter.class)
                .build();
    }
}
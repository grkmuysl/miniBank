package com.gorkemuysal.minibank.gateway.ratelimit;

import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Per-user limits, keyed by the token's "sub". Runs INSIDE the Spring Security chain,
 * right after the bearer token has been validated.
 * <p>
 * Intentionally NOT a @Component: Spring Boot registers every Filter bean as a servlet filter.
 * Combined with addFilterAfter(...) in SecurityConfig, it would run twice per request
 * and users would hit the limit at half the configured rate.
 */
@Slf4j
public class UserRateLimitFilter extends OncePerRequestFilter {

    private static final String TRANSFERS_PATH = "/api/v1/transfers";

    private final BucketStore bucketStore;
    private final RateLimitResponder responder;

    public UserRateLimitFilter(BucketStore bucketStore, RateLimitResponder responder) {
        this.bucketStore = bucketStore;
        this.responder = responder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth) || !request.getRequestURI().startsWith("/api/")) {
            // No valid token (authorization will reject it later) or not an API path.
            chain.doFilter(request, response);
            return;
        }
        String userId = jwtAuth.getToken().getSubject();

        // 1) General limit for all API calls of this user.
        ConsumptionProbe general = bucketStore
                .resolve(RateLimitPolicies.USER_PREFIX + userId, RateLimitPolicies.USER)
                .tryConsumeAndReturnRemaining(1);
        if (!general.isConsumed()) {
            log.warn("User rate limit exceeded for {}", userId);
            responder.reject(response, general, "Too many requests");
            return;
        }
        responder.writeRemaining(response, general);

        // 2) Stricter limit for creating transfers.
        if (HttpMethod.POST.matches(request.getMethod()) && TRANSFERS_PATH.equals(request.getRequestURI())) {
            ConsumptionProbe transfer = bucketStore
                    .resolve(RateLimitPolicies.TRANSFER_PREFIX + userId, RateLimitPolicies.TRANSFER)
                    .tryConsumeAndReturnRemaining(1);
            if (!transfer.isConsumed()) {
                // A burst of transfer attempts can mean a compromised account: worth a WARN.
                log.warn("Transfer rate limit exceeded for {}", userId);
                responder.reject(response, transfer, "Too many transfer requests");
                return;
            }
            // Show the stricter (lower) remaining count for this endpoint.
            responder.writeRemaining(response, transfer);
        }

        chain.doFilter(request, response);
    }
}
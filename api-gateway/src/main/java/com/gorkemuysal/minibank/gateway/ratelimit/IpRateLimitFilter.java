package com.gorkemuysal.minibank.gateway.ratelimit;

import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Per-IP limit, applied right after RequestIdFilter and BEFORE Spring Security.
 * Rejecting here is cheap: no JWT signature verification is done for flooded requests.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class IpRateLimitFilter extends OncePerRequestFilter {

    private final BucketStore bucketStore;
    private final RateLimitResponder responder;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // Deliberately NOT X-Forwarded-For: clients can put anything there and invent a new IP
        // per request. Only trust it when a known proxy/load balancer in front of us sets it.
        String ip = request.getRemoteAddr();

        ConsumptionProbe probe = bucketStore
                .resolve(RateLimitPolicies.IP_PREFIX + ip, RateLimitPolicies.IP)
                .tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            log.warn("IP rate limit exceeded for {}", ip);
            responder.reject(response, probe, "Too many requests from this address");
            return;
        }
        chain.doFilter(request, response);
    }
}
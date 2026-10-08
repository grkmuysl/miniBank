package com.gorkemuysal.minibank.gateway.ratelimit;

import com.gorkemuysal.minibank.gateway.RequestIdFilter;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes rate-limit headers and the 429 response. Filters run before any controller,
 * so GlobalExceptionHandler-style handling is not available here; we write the body ourselves.
 */
@Component
public class RateLimitResponder {

    public static final String REMAINING_HEADER = "X-RateLimit-Remaining";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    public void writeRemaining(HttpServletResponse response, ConsumptionProbe probe) {
        response.setHeader(REMAINING_HEADER, String.valueOf(probe.getRemainingTokens()));
    }

    public void reject(HttpServletResponse response, ConsumptionProbe probe, String detail) throws IOException {
        // Round up: telling a client "retry in 0 seconds" would invite an immediate, failing retry.
        long retryAfterSeconds = Math.max(1, (probe.getNanosToWaitForRefill() + 999_999_999L) / 1_000_000_000L);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "about:blank");
        body.put("title", "Too Many Requests");
        body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("detail", detail);
        body.put("code", "RATE_LIMITED");
        body.put("requestId", MDC.get(RequestIdFilter.MDC_KEY));

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setHeader(REMAINING_HEADER, "0");
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(JSON.writeValueAsString(body));
    }
}
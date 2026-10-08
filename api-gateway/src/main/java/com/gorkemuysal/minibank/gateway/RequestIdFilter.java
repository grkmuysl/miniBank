package com.gorkemuysal.minibank.gateway;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Assigns every incoming request a request ID, logs with it (MDC), returns it to the client
 * and forwards it to the downstream service. Runs before Spring Security, so even
 * rejected (401) requests get an ID.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    // Accept a client-provided ID only if it is short and harmless; otherwise generate one.
    // Prevents log injection (newlines, fake log lines) and oversized values.
    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9-]{1,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String requestId = (incoming != null && VALID.matcher(incoming).matches())
                ? incoming
                : UUID.randomUUID().toString();

        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        try {
            // The gateway forwards the servlet request's headers as they are,
            // so wrapping the request is enough to inject/replace the header.
            chain.doFilter(new RequestIdRequestWrapper(request, requestId), response);
        } finally {
            // Tomcat reuses threads; without this the next request could inherit our ID.
            MDC.remove(MDC_KEY);
        }
    }
}
package com.gorkemuysal.minibank.gateway;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Makes the request look as if it always carried our (validated or generated) X-Request-Id. */
class RequestIdRequestWrapper extends HttpServletRequestWrapper {

    private final String requestId;

    RequestIdRequestWrapper(HttpServletRequest request, String requestId) {
        super(request);
        this.requestId = requestId;
    }

    @Override
    public String getHeader(String name) {
        return RequestIdFilter.HEADER.equalsIgnoreCase(name) ? requestId : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        return RequestIdFilter.HEADER.equalsIgnoreCase(name)
                ? Collections.enumeration(List.of(requestId))
                : super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> names = new LinkedHashSet<>(Collections.list(super.getHeaderNames()));
        // Header names are case-insensitive: drop a client's "x-request-id" before adding ours,
        // otherwise the header could be forwarded twice.
        names.removeIf(RequestIdFilter.HEADER::equalsIgnoreCase);
        names.add(RequestIdFilter.HEADER);
        return Collections.enumeration(names);
    }
}
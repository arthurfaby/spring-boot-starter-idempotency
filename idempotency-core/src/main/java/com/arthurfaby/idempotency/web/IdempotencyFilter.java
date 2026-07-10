package com.arthurfaby.idempotency.web;

import com.arthurfaby.idempotency.config.IdempotencyProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Wraps eligible requests so the body is re-readable (for fingerprinting) and the
 * response is captured (for replay). Runs once per request, only for configured
 * methods carrying the idempotency header — other requests pass through untouched.
 */
public class IdempotencyFilter extends OncePerRequestFilter {

    private final IdempotencyProperties properties;

    public IdempotencyFilter(IdempotencyProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (!isEligible(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        CachedBodyRequestWrapper wrappedRequest = new CachedBodyRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            wrappedResponse.copyBodyToResponse();
        }
    }

    private boolean isEligible(HttpServletRequest request) {
        if (request.getHeader(properties.headerName()) == null) {
            return false;
        }
        return properties.methods().stream().anyMatch(method -> method.equalsIgnoreCase(request.getMethod()));
    }
}

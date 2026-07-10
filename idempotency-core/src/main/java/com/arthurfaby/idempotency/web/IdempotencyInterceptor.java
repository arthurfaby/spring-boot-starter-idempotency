package com.arthurfaby.idempotency.web;

import com.arthurfaby.idempotency.annotation.Idempotent;
import com.arthurfaby.idempotency.config.IdempotencyProperties;
import com.arthurfaby.idempotency.store.IdempotencyStore;
import com.arthurfaby.idempotency.store.model.RequestFingerprint;
import com.arthurfaby.idempotency.store.model.Reservation;
import com.arthurfaby.idempotency.store.model.StoredRecord;
import com.arthurfaby.idempotency.store.model.StoredResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.util.StreamUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

/**
 * The idempotency decision point. On {@code preHandle} it reads {@link Idempotent}
 * and the key header, reserves the key and maps the {@link Reservation} to an HTTP
 * behaviour. On {@code afterCompletion} it stores the captured response, or releases
 * the key on failure so the request can be retried.
 */
public class IdempotencyInterceptor implements HandlerInterceptor {

    private static final String KEY_ATTRIBUTE = IdempotencyInterceptor.class.getName() + ".KEY";

    private final IdempotencyStore store;
    private final IdempotencyProperties properties;

    public IdempotencyInterceptor(IdempotencyStore store, IdempotencyProperties properties) {
        this.store = store;
        this.properties = properties;
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler)
            throws IOException {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        Idempotent annotation = handlerMethod.getMethodAnnotation(Idempotent.class);
        if (annotation == null) {
            return true; // endpoint not annotated → normal flow
        }
        String key = request.getHeader(properties.headerName());
        if (key == null || key.isBlank()) {
            return true; // no key → behave like a normal request (fail-open)
        }

        RequestFingerprint fingerprint =
                RequestFingerprint.of(request.getMethod(), request.getRequestURI(), readBody(request));
        Duration ttl = resolveTtl(annotation);

        Reservation reservation = store.reserve(key, fingerprint, ttl);
        return switch (reservation.outcome()) {
            case NEW -> {
                request.setAttribute(KEY_ATTRIBUTE, key);
                yield true; // let the controller run
            }
            case IN_PROGRESS -> {
                writeError(
                        response, HttpStatus.CONFLICT, "A request with this Idempotency-Key is already in progress.");
                yield false;
            }
            case MISMATCH -> {
                writeError(
                        response,
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "This Idempotency-Key was already used with a different request.");
                yield false;
            }
            case REPLAY -> {
                StoredRecord.Completed completed = (StoredRecord.Completed) reservation.record();
                replay(response, completed.response());
                yield false;
            }
        };
    }

    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            Exception ex) {
        String key = (String) request.getAttribute(KEY_ATTRIBUTE);
        if (key == null) {
            return; // this request did not reserve a key
        }
        if (ex != null || response.getStatus() >= 500) {
            store.release(key); // failure → free the key so the client can retry
            return;
        }
        store.complete(key, captureResponse(response));
    }

    private byte[] readBody(HttpServletRequest request) throws IOException {
        CachedBodyRequestWrapper cached = WebUtils.getNativeRequest(request, CachedBodyRequestWrapper.class);
        return cached != null ? cached.getBody() : StreamUtils.copyToByteArray(request.getInputStream());
    }

    private Duration resolveTtl(Idempotent annotation) {
        String ttl = annotation.ttl();
        return (ttl == null || ttl.isBlank()) ? properties.defaultTtl() : DurationStyle.detectAndParse(ttl);
    }

    private StoredResponse captureResponse(HttpServletResponse response) {
        ContentCachingResponseWrapper wrapper =
                WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
        if (wrapper == null) {
            return new StoredResponse(response.getStatus(), Map.of(), new byte[0]);
        }
        Map<String, List<String>> headers = new LinkedHashMap<>();
        for (String name : wrapper.getHeaderNames()) {
            if (!HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name)) {
                headers.put(name, new ArrayList<>(wrapper.getHeaders(name)));
            }
        }
        return new StoredResponse(wrapper.getStatus(), headers, wrapper.getContentAsByteArray());
    }

    private void replay(HttpServletResponse response, StoredResponse stored) throws IOException {
        response.setStatus(stored.status());
        stored.headers().forEach((name, values) -> values.forEach(value -> response.addHeader(name, value)));
        response.getOutputStream().write(stored.body());
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("text/plain;charset=UTF-8");
        response.getOutputStream().write(message.getBytes(StandardCharsets.UTF_8));
    }
}

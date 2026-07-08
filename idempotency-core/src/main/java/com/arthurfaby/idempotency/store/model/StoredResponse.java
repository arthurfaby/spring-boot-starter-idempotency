package com.arthurfaby.idempotency.store.model;

import java.util.List;
import java.util.Map;

/**
 * The captured HTTP response to replay on subsequent requests with the same key.
 *
 * @param status  HTTP status code
 * @param headers response headers to replay
 * @param body    raw response body
 */
public record StoredResponse(int status, Map<String, List<String>> headers, byte[] body) {

    public StoredResponse {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        body = body == null ? new byte[0] : body.clone();
    }

    /** Defensive copy so the stored body can't be mutated by callers. */
    @Override
    public byte[] body() {
        return body.clone();
    }
}
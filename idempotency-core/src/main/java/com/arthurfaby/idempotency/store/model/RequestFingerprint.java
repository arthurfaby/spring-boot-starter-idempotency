package com.arthurfaby.idempotency.store.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Identity of a request bound to an idempotency key: a hash of the HTTP method,
 * path and body. Two requests sharing a key must share the same fingerprint;
 * otherwise the key is being reused for a different request.
 *
 * @param hash lowercase hex SHA-256 of {@code method + path + body}
 */
public record RequestFingerprint(String hash) {

    public RequestFingerprint {
        Objects.requireNonNull(hash, "hash must not be null");
    }

    /** Computes the fingerprint of a request. */
    public static RequestFingerprint of(String method, String path, byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(method.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
            digest.update(path.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
            if (body != null) {
                digest.update(body);
            }
            return new RequestFingerprint(HexFormat.of().formatHex(digest.digest()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

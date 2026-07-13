package com.arthurfaby.idempotency.store.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class RequestFingerprintTest {

    @Test
    void sameRequestProducesSameFingerprint() {
        var a = RequestFingerprint.of("POST", "/payments", body("{\"amount\":10}"));
        var b = RequestFingerprint.of("POST", "/payments", body("{\"amount\":10}"));
        assertThat(a).isEqualTo(b);
    }

    @Test
    void differentBodyProducesDifferentFingerprint() {
        var a = RequestFingerprint.of("POST", "/payments", body("{\"amount\":10}"));
        var b = RequestFingerprint.of("POST", "/payments", body("{\"amount\":99}"));
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void differentMethodProducesDifferentFingerprint() {
        var a = RequestFingerprint.of("POST", "/payments", body("{\"amount\":10}"));
        var b = RequestFingerprint.of("PATCH", "/payments", body("{\"amount\":10}"));
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void differentPathProducesDifferentFingerprint() {
        var a = RequestFingerprint.of("POST", "/payments", new byte[0]);
        var b = RequestFingerprint.of("POST", "/refunds", new byte[0]);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void hashIsLowercaseHexSha256() {
        var fingerprint = RequestFingerprint.of("GET", "/", new byte[0]);
        assertThat(fingerprint.hash()).hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    void nullBodyIsAllowed() {
        var fingerprint = RequestFingerprint.of("GET", "/health", null);
        assertThat(fingerprint.hash()).hasSize(64);
    }

    private static byte[] body(String json) {
        return json.getBytes(StandardCharsets.UTF_8);
    }
}

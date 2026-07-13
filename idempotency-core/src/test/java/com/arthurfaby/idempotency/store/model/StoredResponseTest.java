package com.arthurfaby.idempotency.store.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StoredResponseTest {

    @Test
    void nullHeadersAndBodyDefaultToEmpty() {
        var response = new StoredResponse(204, null, null);

        assertThat(response.headers()).isEmpty();
        assertThat(response.body()).isEmpty();
    }

    @Test
    void bodyIsReturnedAsADefensiveCopy() {
        byte[] source = {1, 2, 3};
        var response = new StoredResponse(200, Map.of("Content-Type", List.of("text/plain")), source);

        response.body()[0] = 99; // mutating the returned array...
        source[1] = 42; // ...or the original array...

        assertThat(response.body()).isEqualTo(new byte[] {1, 2, 3}); // ...leaves the stored body intact
    }
}

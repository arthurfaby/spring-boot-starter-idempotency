package com.arthurfaby.idempotency.store;

import static org.assertj.core.api.Assertions.assertThat;

import com.arthurfaby.idempotency.store.model.RequestFingerprint;
import com.arthurfaby.idempotency.store.model.Reservation;
import com.arthurfaby.idempotency.store.model.StoredRecord;
import com.arthurfaby.idempotency.store.model.StoredResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class InMemoryIdempotencyStoreTest {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final RequestFingerprint fingerprint =
            RequestFingerprint.of("POST", "/payments", "{\"amount\":10}".getBytes());

    @Test
    void reserveOnFreeKeyReturnsNew() {
        var store = new InMemoryIdempotencyStore();

        Reservation reservation = store.reserve("key-1", fingerprint, TTL);

        assertThat(reservation.outcome()).isEqualTo(Reservation.Outcome.NEW);
        assertThat(store.find("key-1")).get().isInstanceOf(StoredRecord.InProgress.class);
    }

    @Test
    void reserveWhileInProgressReturnsInProgress() {
        var store = new InMemoryIdempotencyStore();
        store.reserve("key-1", fingerprint, TTL);

        Reservation reservation = store.reserve("key-1", fingerprint, TTL);

        assertThat(reservation.outcome()).isEqualTo(Reservation.Outcome.IN_PROGRESS);
    }

    @Test
    void reserveAfterCompleteWithSameRequestReplaysStoredResponse() {
        var store = new InMemoryIdempotencyStore();
        store.reserve("key-1", fingerprint, TTL);
        var stored =
                new StoredResponse(201, Map.of("Content-Type", List.of("application/json")), "{\"id\":1}".getBytes());
        store.complete("key-1", stored);

        Reservation reservation = store.reserve("key-1", fingerprint, TTL);

        assertThat(reservation.outcome()).isEqualTo(Reservation.Outcome.REPLAY);
        assertThat(reservation.record()).isInstanceOf(StoredRecord.Completed.class);
        var completed = (StoredRecord.Completed) reservation.record();
        assertThat(completed.response().status()).isEqualTo(201);
        assertThat(completed.response().body()).isEqualTo("{\"id\":1}".getBytes());
    }

    @Test
    void reserveAfterCompleteWithDifferentRequestIsMismatch() {
        var store = new InMemoryIdempotencyStore();
        store.reserve("key-1", fingerprint, TTL);
        store.complete("key-1", new StoredResponse(201, Map.of(), new byte[0]));

        var otherRequest = RequestFingerprint.of("POST", "/payments", "{\"amount\":999}".getBytes());
        Reservation reservation = store.reserve("key-1", otherRequest, TTL);

        assertThat(reservation.outcome()).isEqualTo(Reservation.Outcome.MISMATCH);
    }

    @Test
    void expiredKeyIsTreatedAsFree() {
        var clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        var store = new InMemoryIdempotencyStore(clock);
        store.reserve("key-1", fingerprint, Duration.ofMinutes(1));

        clock.advance(Duration.ofMinutes(2)); // past the TTL

        assertThat(store.find("key-1")).isEmpty();
        assertThat(store.reserve("key-1", fingerprint, TTL).outcome()).isEqualTo(Reservation.Outcome.NEW);
    }

    @Test
    void onlyOneConcurrentReserveWinsTheKey() throws InterruptedException {
        var store = new InMemoryIdempotencyStore();
        int threads = 64;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        var startGate = new CountDownLatch(1);
        var doneGate = new CountDownLatch(threads);
        var newCount = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    Reservation reservation = store.reserve("same-key", fingerprint, TTL);
                    if (reservation.outcome() == Reservation.Outcome.NEW) {
                        newCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown();
        assertThat(doneGate.await(5, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        assertThat(newCount).hasValue(1);
    }
}

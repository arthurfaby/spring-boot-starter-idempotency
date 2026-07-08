package com.arthurfaby.idempotency.store;

import com.arthurfaby.idempotency.store.model.Reservation;
import com.arthurfaby.idempotency.store.model.RequestFingerprint;
import com.arthurfaby.idempotency.store.model.StoredRecord;
import com.arthurfaby.idempotency.store.model.StoredResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe in-memory {@link IdempotencyStore} backed by a {@link ConcurrentHashMap}.
 *
 * <p>Atomicity is guaranteed per key via {@link ConcurrentHashMap#compute}: the
 * mapping function runs under the key's bin lock, so concurrent {@code reserve}
 * calls for the same key are serialized and exactly one returns
 * {@link Reservation.Outcome#NEW}. Expired records are purged lazily on access.
 */
public class InMemoryIdempotencyStore implements IdempotencyStore {

    private final Map<String, StoredRecord> records = new ConcurrentHashMap<>();
    private final Clock clock;

    /** Uses the system UTC clock. */
    public InMemoryIdempotencyStore() {
        this(Clock.systemUTC());
    }

    /** Uses a custom clock (for testing). */
    public InMemoryIdempotencyStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Reservation reserve(String key, RequestFingerprint fingerprint, Duration ttl) {
        Instant now = clock.instant();
        AtomicReference<Reservation> verdict = new AtomicReference<>();

        records.compute(key, (k, existing) -> {
            // Free slot (or expired) → reserve it as IN_PROGRESS.
            if (existing == null || existing.isExpired(now)) {
                verdict.set(Reservation.created());
                return new StoredRecord.InProgress(fingerprint, now.plus(ttl));
            }
            // Otherwise decide from the existing record's type (exhaustive switch).
            switch (existing) {
                case StoredRecord.InProgress inProgress ->
                        verdict.set(Reservation.inProgress(inProgress));
                case StoredRecord.Completed completed -> verdict.set(
                        completed.fingerprint().equals(fingerprint)
                                ? Reservation.replay(completed)
                                : Reservation.mismatch(completed));
            }
            return existing; // untouched
        });

        return verdict.get();
    }

    @Override
    public void complete(String key, StoredResponse response) {
        records.compute(key, (k, existing) -> {
            // Nothing to complete if the key vanished or expired meanwhile.
            if (existing == null || existing.isExpired(clock.instant())) {
                return existing;
            }
            // Keep the original expiry: TTL counts from the first request.
            return new StoredRecord.Completed(existing.fingerprint(), response, existing.expiresAt());
        });
    }

    @Override
    public Optional<StoredRecord> find(String key) {
        StoredRecord record = records.get(key);
        if (record == null) {
            return Optional.empty();
        }
        if (record.isExpired(clock.instant())) {
            records.remove(key, record); // lazy purge, only if unchanged
            return Optional.empty();
        }
        return Optional.of(record);
    }
}
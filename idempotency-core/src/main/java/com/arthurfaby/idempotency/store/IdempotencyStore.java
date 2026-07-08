package com.arthurfaby.idempotency.store;

import com.arthurfaby.idempotency.store.model.Reservation;
import com.arthurfaby.idempotency.store.model.RequestFingerprint;
import com.arthurfaby.idempotency.store.model.StoredRecord;
import com.arthurfaby.idempotency.store.model.StoredResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Storage abstraction for idempotency keys — the SPI of the library.
 *
 * <p>Implementations MUST make {@link #reserve} atomic per key: given concurrent
 * calls for the same key, exactly one must return {@link Reservation.Outcome#NEW}.
 */
public interface IdempotencyStore {

    /**
     * Atomically reserves the key if free.
     *
     * @param key         the idempotency key
     * @param fingerprint identity of the current request
     * @param ttl         how long the key is retained
     * @return the reservation verdict
     */
    Reservation reserve(String key, RequestFingerprint fingerprint, Duration ttl);

    /** Stores the response and marks the key {@code COMPLETED}. */
    void complete(String key, StoredResponse response);

    /** Returns the (non-expired) record for the key, if any. */
    Optional<StoredRecord> find(String key);
}
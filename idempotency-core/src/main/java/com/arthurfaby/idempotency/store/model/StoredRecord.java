package com.arthurfaby.idempotency.store.model;

import java.time.Instant;

/**
 * What the store keeps for a key. A sealed hierarchy makes the two possible
 * states explicit and mutually exclusive — no nullable "response" field and no
 * separate state enum to keep in sync.
 */
public sealed interface StoredRecord
        permits StoredRecord.InProgress, StoredRecord.Completed {

    /** Identity of the request that reserved the key. */
    RequestFingerprint fingerprint();

    /** When the key expires (TTL counted from the first request). */
    Instant expiresAt();

    /** True if the record's TTL has elapsed at {@code now}. */
    default boolean isExpired(Instant now) {
        return now.isAfter(expiresAt());
    }

    /** The request is being processed; no response yet. */
    record InProgress(RequestFingerprint fingerprint, Instant expiresAt)
            implements StoredRecord {
    }

    /** The request finished; its response is stored for replay. */
    record Completed(RequestFingerprint fingerprint, StoredResponse response, Instant expiresAt)
            implements StoredRecord {
    }
}
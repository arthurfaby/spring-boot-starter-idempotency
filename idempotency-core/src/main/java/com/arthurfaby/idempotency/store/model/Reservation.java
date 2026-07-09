package com.arthurfaby.idempotency.store.model;

/**
 * Verdict returned by {@code IdempotencyStore.reserve(...)}. The web layer maps
 * each outcome to an HTTP behaviour.
 *
 * @param outcome what happened
 * @param record  the existing record (null for {@link Outcome#NEW})
 */
public record Reservation(Outcome outcome, StoredRecord record) {

    public enum Outcome {
        /** Key was free: caller may execute the request. */
        NEW,
        /** Key is already being processed → 409 Conflict. */
        IN_PROGRESS,
        /** Key completed with the same request → replay the stored response. */
        REPLAY,
        /** Key completed with a different request → 422 Unprocessable Entity. */
        MISMATCH
    }

    public static Reservation created() {
        return new Reservation(Outcome.NEW, null);
    }

    public static Reservation inProgress(StoredRecord.InProgress record) {
        return new Reservation(Outcome.IN_PROGRESS, record);
    }

    public static Reservation replay(StoredRecord.Completed record) {
        return new Reservation(Outcome.REPLAY, record);
    }

    public static Reservation mismatch(StoredRecord.Completed record) {
        return new Reservation(Outcome.MISMATCH, record);
    }
}

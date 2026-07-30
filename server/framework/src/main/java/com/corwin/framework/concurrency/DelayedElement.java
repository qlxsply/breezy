package com.corwin.framework.concurrency;

import com.corwin.framework.util.HighDate;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * A generic delayed element wrapping a payload and an expiration timestamp.
 * <p>
 * Equality is based solely on the payload (via {@link Objects#hash(Object...)}),
 * which enables duplicate removal in {@link DelayQueueProcessor#submit(DelayedElement)}.
 *
 * @param <T>        the payload type
 * @param payload    the task payload (non-null)
 * @param expireTime the instant at which this element becomes eligible for processing (non-null)
 * @author Corwin 2026/3/16
 */
public record DelayedElement<T>(
        T payload,
        Instant expireTime
) implements Delayed {

    public DelayedElement(T payload, Instant expireTime) {
        this.payload = Objects.requireNonNull(payload);
        this.expireTime = Objects.requireNonNull(expireTime);
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long diff = expireTime.toEpochMilli() - HighDate.mockInstant().toEpochMilli();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@NotNull Delayed o) {
        if (this == o) {
            return 0;
        }
        if (o instanceof DelayedElement<?> other) {
            return this.expireTime.compareTo(other.expireTime);
        }
        long diff = this.getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS);
        return Long.compare(diff, 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DelayedElement<?> that = (DelayedElement<?>) o;
        return Objects.equals(payload, that.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(payload);
    }
}

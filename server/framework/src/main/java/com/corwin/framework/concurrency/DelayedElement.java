package com.corwin.framework.concurrency;

import com.corwin.framework.util.HighDate;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 通用的延迟元素包装类
 *
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

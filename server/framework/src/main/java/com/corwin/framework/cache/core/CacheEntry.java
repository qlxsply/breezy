package com.corwin.framework.cache.core;

import com.corwin.framework.cache.CacheDataType;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;

/**
 * 本地缓存条目。
 *
 * @author Corwin 2026/4/19
 */
@Getter
public final class CacheEntry {

    private final CacheDataType dataType;
    private final Object value;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant expireAt;

    public CacheEntry(CacheDataType dataType, Object value, Instant createdAt, Instant updatedAt, Instant expireAt) {
        this.dataType = dataType;
        this.value = value;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.expireAt = expireAt;
    }

    public boolean isExpired(Instant now) {
        return expireAt != null && !expireAt.isAfter(now);
    }

    public boolean isPersistent() {
        return expireAt == null;
    }

    public Duration remainingTtl(Instant now) {
        if (expireAt == null) {
            return null;
        }
        return Duration.between(now, expireAt);
    }

    public CacheEntry withValue(Object newValue, Instant now) {
        return new CacheEntry(dataType, newValue, createdAt, now, expireAt);
    }

    public CacheEntry withExpireAt(Instant newExpireAt, Instant now) {
        return new CacheEntry(dataType, value, createdAt, now, newExpireAt);
    }

    public static CacheEntry create(CacheDataType dataType, Object value, Instant now, Instant expireAt) {
        return new CacheEntry(dataType, value, now, now, expireAt);
    }
}

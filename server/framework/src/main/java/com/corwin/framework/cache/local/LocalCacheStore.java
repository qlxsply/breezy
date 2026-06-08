package com.corwin.framework.cache.local;

import com.corwin.framework.cache.CacheDataType;
import com.corwin.framework.cache.CacheTtlResult;
import com.corwin.framework.cache.core.CacheEntry;

import java.time.Duration;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 本地缓存存储抽象。
 *
 * @author Corwin 2026/4/19
 */
public interface LocalCacheStore {

    <R> R read(CacheDataType dataType, String key, Function<CacheEntry, R> reader, Supplier<R> absentSupplier);

    <R> R write(CacheDataType dataType, String key, BiFunction<CacheEntry, java.time.Instant, LocalCacheWriteResult<R>> writer);

    boolean exists(CacheDataType dataType, String key);

    boolean delete(CacheDataType dataType, String key);

    boolean expire(CacheDataType dataType, String key, Duration ttl);

    CacheTtlResult ttl(CacheDataType dataType, String key);
}

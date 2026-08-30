package com.corwin.framework.cache.local;

import com.corwin.framework.cache.core.CacheEntry;

/**
 * Result of a local cache write operation with the entry, result value, and removal flag.
 *
 * @author Corwin 2026/4/19
 */
public record LocalCacheWriteResult<R>(CacheEntry entry, R result, boolean remove) {

  public static <R> LocalCacheWriteResult<R> keep(CacheEntry entry, R result) {
    return new LocalCacheWriteResult<>(entry, result, false);
  }

  public static <R> LocalCacheWriteResult<R> remove(R result) {
    return new LocalCacheWriteResult<>(null, result, true);
  }
}

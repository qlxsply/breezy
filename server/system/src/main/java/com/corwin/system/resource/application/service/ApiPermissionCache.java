package com.corwin.system.resource.application.service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * In-memory cache for API permission sets keyed by role ID.
 *
 * <p>Uses a {@link ConcurrentHashMap} to store and retrieve permission code sets, with support for
 * lazy loading via a {@link Supplier} and selective invalidation.
 *
 * @author Corwin 2026/1/23
 */
@Component
public class ApiPermissionCache {

  private final Map<Long, Set<String>> cache = new ConcurrentHashMap<>();

  /**
   * Returns the cached permission set for the given key, loading it via the supplier if absent.
   *
   * @param key the cache key (typically a role or user ID)
   * @param loader the loader function to produce the value on cache miss
   * @return the cached or freshly loaded set of permission codes
   */
  public Set<String> getOrLoad(Long key, Supplier<Set<String>> loader) {
    return cache.computeIfAbsent(key, __ -> loader.get());
  }

  /**
   * Evicts the cached entry for the given key.
   *
   * @param key the cache key to remove
   */
  public void clear(Long key) {
    cache.remove(key);
  }

  /** Evicts all cached entries. */
  public void clearAll() {
    cache.clear();
  }
}

package com.corwin.framework.cache;

import java.time.Duration;

/**
 * Cache key-level operations (exists, delete, expire, ttl).
 *
 * @author Corwin 2026/4/19
 */
public interface CacheKeyOps {

    boolean exists(String key);

    boolean delete(String key);

    boolean expire(String key, Duration ttl);

    CacheTtlResult ttl(String key);
}

package com.corwin.framework.cache;

/**
 * Cache deployment mode — local-only, Redis-only, or combined local+Redis.
 *
 * @author Corwin 2026/4/19
 */
public enum CacheMode {
    LOCAL,
    REDIS,
    LOCAL_REDIS
}

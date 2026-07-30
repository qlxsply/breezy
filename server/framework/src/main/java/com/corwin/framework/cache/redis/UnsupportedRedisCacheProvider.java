package com.corwin.framework.cache.redis;

import com.corwin.framework.cache.CacheHashOps;
import com.corwin.framework.cache.CacheListOps;
import com.corwin.framework.cache.CacheObjectOps;
import com.corwin.framework.cache.CacheSetOps;
import com.corwin.framework.cache.CacheSortedSetOps;
import com.corwin.framework.cache.CacheStringOps;
import com.corwin.framework.cache.core.CacheProvider;
import com.corwin.framework.error.BizException;
import com.corwin.framework.error.CacheError;

/**
 * Placeholder {@link com.corwin.framework.cache.core.CacheProvider} for the Redis mode (not yet implemented).
 *
 * @author Corwin 2026/4/19
 */
public class UnsupportedRedisCacheProvider implements CacheProvider {

    @Override
    public CacheStringOps stringOps() {
        throw unsupported();
    }

    @Override
    public <T> CacheObjectOps<T> objectOps(Class<T> valueType) {
        throw unsupported();
    }

    @Override
    public <T> CacheHashOps<T> hashOps(Class<T> valueType) {
        throw unsupported();
    }

    @Override
    public <T> CacheListOps<T> listOps(Class<T> valueType) {
        throw unsupported();
    }

    @Override
    public <T> CacheSetOps<T> setOps(Class<T> valueType) {
        throw unsupported();
    }

    @Override
    public <T> CacheSortedSetOps<T> sortedSetOps(Class<T> valueType) {
        throw unsupported();
    }

    private BizException unsupported() {
        return new BizException(
                "Redis cache provider is not implemented yet. Current supported cache mode: LOCAL.",
                CacheError.CACHE_MODE_NOT_IMPLEMENTED
        );
    }
}

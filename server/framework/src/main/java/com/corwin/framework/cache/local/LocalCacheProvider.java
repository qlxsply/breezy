package com.corwin.framework.cache.local;

import com.corwin.framework.cache.*;
import com.corwin.framework.cache.core.CacheKeyValidator;
import com.corwin.framework.cache.core.CacheProvider;

/**
 * {@link com.corwin.framework.cache.core.CacheProvider} implementation for local (Caffeine-backed) caching.
 *
 * @author Corwin 2026/4/19
 */
public class LocalCacheProvider implements CacheProvider {

    private final LocalCacheStore store;
    private final CacheKeyValidator validator;

    public LocalCacheProvider(LocalCacheStore store, CacheKeyValidator validator) {
        this.store = store;
        this.validator = validator;
    }

    @Override
    public CacheStringOps stringOps() {
        return new LocalCacheStringOps(store, validator);
    }

    @Override
    public <T> CacheObjectOps<T> objectOps(Class<T> valueType) {
        return new LocalCacheObjectOps<>(store, validator, valueType);
    }

    @Override
    public <T> CacheHashOps<T> hashOps(Class<T> valueType) {
        return new LocalCacheHashOps<>(store, validator, valueType);
    }

    @Override
    public <T> CacheListOps<T> listOps(Class<T> valueType) {
        return new LocalCacheListOps<>(store, validator, valueType);
    }

    @Override
    public <T> CacheSetOps<T> setOps(Class<T> valueType) {
        return new LocalCacheSetOps<>(store, validator, valueType);
    }

    @Override
    public <T> CacheSortedSetOps<T> sortedSetOps(Class<T> valueType) {
        return new LocalCacheSortedSetOps<>(store, validator, valueType);
    }
}

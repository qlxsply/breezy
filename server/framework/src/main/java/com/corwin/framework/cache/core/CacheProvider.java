package com.corwin.framework.cache.core;

import com.corwin.framework.cache.*;

/**
 * 缓存模式 provider。
 *
 * @author Corwin 2026/4/19
 */
public interface CacheProvider {

    CacheStringOps stringOps();

    <T> CacheObjectOps<T> objectOps(Class<T> valueType);

    <T> CacheHashOps<T> hashOps(Class<T> valueType);

    <T> CacheListOps<T> listOps(Class<T> valueType);

    <T> CacheSetOps<T> setOps(Class<T> valueType);

    <T> CacheSortedSetOps<T> sortedSetOps(Class<T> valueType);
}

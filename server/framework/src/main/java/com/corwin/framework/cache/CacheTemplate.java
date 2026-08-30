package com.corwin.framework.cache;

/**
 * Entry point for typed cache operations by mode.
 *
 * @author Corwin 2026/4/19
 */
public interface CacheTemplate {

  CacheStringOps stringOps(CacheMode mode);

  <T> CacheObjectOps<T> objectOps(CacheMode mode, Class<T> valueType);

  <T> CacheHashOps<T> hashOps(CacheMode mode, Class<T> valueType);

  <T> CacheListOps<T> listOps(CacheMode mode, Class<T> valueType);

  <T> CacheSetOps<T> setOps(CacheMode mode, Class<T> valueType);

  <T> CacheSortedSetOps<T> sortedSetOps(CacheMode mode, Class<T> valueType);
}

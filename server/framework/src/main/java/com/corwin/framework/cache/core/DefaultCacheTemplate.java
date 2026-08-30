package com.corwin.framework.cache.core;

import com.corwin.framework.cache.*;
import com.corwin.framework.error.BizException;
import com.corwin.framework.error.CacheError;

/**
 * Default {@link CacheTemplate} implementation that delegates to the appropriate provider.
 *
 * @author Corwin 2026/4/19
 */
public class DefaultCacheTemplate implements CacheTemplate {

  private final CacheProvider localProvider;
  private final CacheProvider redisProvider;
  private final CacheProvider tieredProvider;

  public DefaultCacheTemplate(
      CacheProvider localProvider, CacheProvider redisProvider, CacheProvider tieredProvider) {
    this.localProvider = localProvider;
    this.redisProvider = redisProvider;
    this.tieredProvider = tieredProvider;
  }

  @Override
  public CacheStringOps stringOps(CacheMode mode) {
    return provider(mode).stringOps();
  }

  @Override
  public <T> CacheObjectOps<T> objectOps(CacheMode mode, Class<T> valueType) {
    return provider(mode).objectOps(valueType);
  }

  @Override
  public <T> CacheHashOps<T> hashOps(CacheMode mode, Class<T> valueType) {
    return provider(mode).hashOps(valueType);
  }

  @Override
  public <T> CacheListOps<T> listOps(CacheMode mode, Class<T> valueType) {
    return provider(mode).listOps(valueType);
  }

  @Override
  public <T> CacheSetOps<T> setOps(CacheMode mode, Class<T> valueType) {
    return provider(mode).setOps(valueType);
  }

  @Override
  public <T> CacheSortedSetOps<T> sortedSetOps(CacheMode mode, Class<T> valueType) {
    return provider(mode).sortedSetOps(valueType);
  }

  private CacheProvider provider(CacheMode mode) {
    if (mode == null) {
      throw new BizException(CacheError.CACHE_MODE_REQUIRED);
    }
    return switch (mode) {
      case LOCAL -> localProvider;
      case REDIS -> redisProvider;
      case LOCAL_REDIS -> tieredProvider;
    };
  }
}

package com.corwin.framework.cache.local;

import com.corwin.framework.cache.CacheDataType;
import com.corwin.framework.cache.CacheKeyOps;
import com.corwin.framework.cache.CacheTtlResult;
import com.corwin.framework.cache.core.CacheKeyValidator;
import java.time.Duration;

/**
 * Base class for local cache key operations (exists, delete, expire, ttl).
 *
 * @author Corwin 2026/4/19
 */
abstract class AbstractLocalCacheOps implements CacheKeyOps {

  protected final LocalCacheStore store;
  protected final CacheKeyValidator validator;
  private final CacheDataType dataType;

  protected AbstractLocalCacheOps(
      LocalCacheStore store, CacheKeyValidator validator, CacheDataType dataType) {
    this.store = store;
    this.validator = validator;
    this.dataType = dataType;
  }

  @Override
  public boolean exists(String key) {
    validator.validateKey(key);
    return store.exists(dataType, key);
  }

  @Override
  public boolean delete(String key) {
    validator.validateKey(key);
    return store.delete(dataType, key);
  }

  @Override
  public boolean expire(String key, Duration ttl) {
    validator.validateKey(key);
    validator.validateTtl(ttl);
    return store.expire(dataType, key, ttl);
  }

  @Override
  public CacheTtlResult ttl(String key) {
    validator.validateKey(key);
    return store.ttl(dataType, key);
  }

  protected CacheDataType dataType() {
    return dataType;
  }
}

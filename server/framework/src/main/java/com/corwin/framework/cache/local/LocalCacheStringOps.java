package com.corwin.framework.cache.local;

import com.corwin.framework.cache.CacheDataType;
import com.corwin.framework.cache.CacheStringOps;
import com.corwin.framework.cache.core.CacheEntry;
import com.corwin.framework.cache.core.CacheKeyValidator;
import com.corwin.framework.error.BizException;
import com.corwin.framework.error.CacheError;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Local cache string operations backed by the store.
 *
 * @author Corwin 2026/4/19
 */
public class LocalCacheStringOps extends AbstractLocalCacheOps implements CacheStringOps {

  public LocalCacheStringOps(LocalCacheStore store, CacheKeyValidator validator) {
    super(store, validator, CacheDataType.STRING);
  }

  @Override
  public Optional<String> get(String key) {
    validator.validateKey(key);
    return store.read(
        dataType(), key, entry -> Optional.of((String) entry.getValue()), Optional::empty);
  }

  @Override
  public void set(String key, String value) {
    set(key, value, null);
  }

  @Override
  public void set(String key, String value, Duration ttl) {
    validator.validateKey(key);
    validator.validateValue(value);
    if (ttl != null) {
      validator.validateTtl(ttl);
    }
    store.write(
        dataType(),
        key,
        (entry, now) -> LocalCacheWriteResult.keep(upsert(entry, value, now, ttl), null));
  }

  @Override
  public Map<String, String> multiGet(Collection<String> keys) {
    validator.validateKeys(keys);
    Map<String, String> result = new LinkedHashMap<>();
    for (String key : keys) {
      get(key).ifPresent(value -> result.put(key, value));
    }
    return result;
  }

  @Override
  public void multiSet(Map<String, String> values) {
    multiSet(values, null);
  }

  @Override
  public void multiSet(Map<String, String> values, Duration ttl) {
    validator.validateMap(values);
    if (ttl != null) {
      validator.validateTtl(ttl);
    }
    for (Map.Entry<String, String> entry : values.entrySet()) {
      set(entry.getKey(), entry.getValue(), ttl);
    }
  }

  @Override
  public long increment(String key) {
    return increment(key, 1L);
  }

  @Override
  public long increment(String key, long delta) {
    validator.validateKey(key);
    return store.write(
        dataType(),
        key,
        (entry, now) -> {
          long current = 0L;
          Instant expireAt = null;
          if (entry != null) {
            current = parseLong(key, (String) entry.getValue());
            expireAt = entry.getExpireAt();
          }
          long updatedValue = current + delta;
          CacheEntry updated =
              entry == null
                  ? CacheEntry.create(dataType(), Long.toString(updatedValue), now, null)
                  : new CacheEntry(
                      dataType(), Long.toString(updatedValue), entry.getCreatedAt(), now, expireAt);
          return LocalCacheWriteResult.keep(updated, updatedValue);
        });
  }

  @Override
  public long decrement(String key) {
    return decrement(key, 1L);
  }

  @Override
  public long decrement(String key, long delta) {
    return increment(key, -delta);
  }

  private CacheEntry upsert(CacheEntry entry, String value, Instant now, Duration ttl) {
    Instant expireAt = ttl == null ? (entry == null ? null : entry.getExpireAt()) : now.plus(ttl);
    if (entry == null) {
      return CacheEntry.create(dataType(), value, now, expireAt);
    }
    return new CacheEntry(dataType(), value, entry.getCreatedAt(), now, expireAt);
  }

  private long parseLong(String key, String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ex) {
      throw new BizException(
          "Cache key '" + key + "' does not contain a valid long value",
          CacheError.CACHE_VALUE_TYPE_MISMATCH);
    }
  }
}

package com.corwin.framework.cache.local;

import com.corwin.framework.cache.CacheDataType;
import com.corwin.framework.cache.CacheHashOps;
import com.corwin.framework.cache.core.CacheEntry;
import com.corwin.framework.cache.core.CacheKeyValidator;
import com.corwin.framework.cache.support.TypeSafeValueCaster;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local cache hash operations backed by {@link java.util.concurrent.ConcurrentHashMap}.
 *
 * @author Corwin 2026/4/19
 */
public class LocalCacheHashOps<T> extends AbstractLocalCacheOps implements CacheHashOps<T> {

  private final Class<T> valueType;

  public LocalCacheHashOps(LocalCacheStore store, CacheKeyValidator validator, Class<T> valueType) {
    super(store, validator, CacheDataType.HASH);
    this.valueType = validator.validateValueType(valueType);
  }

  @Override
  public Optional<T> get(String key, String field) {
    validator.validateKey(key);
    validator.validateField(field);
    return store.read(
        dataType(),
        key,
        entry -> {
          Map<String, Object> values = asMap(entry);
          return Optional.ofNullable(values.get(field))
              .map(value -> TypeSafeValueCaster.cast(value, valueType));
        },
        Optional::empty);
  }

  @Override
  public void put(String key, String field, T value) {
    put(key, field, value, null);
  }

  @Override
  public void put(String key, String field, T value, Duration ttl) {
    validator.validateKey(key);
    validator.validateField(field);
    validator.validateValue(value);
    if (ttl != null) {
      validator.validateTtl(ttl);
    }
    store.write(
        dataType(),
        key,
        (entry, now) -> {
          Map<String, Object> values = entry == null ? new ConcurrentHashMap<>() : asMap(entry);
          values.put(field, value);
          return LocalCacheWriteResult.keep(upsert(entry, values, now, ttl), null);
        });
  }

  @Override
  public Map<String, T> multiGet(String key, Collection<String> fields) {
    validator.validateKey(key);
    if (fields == null) {
      return Map.of();
    }
    for (String field : fields) {
      validator.validateField(field);
    }
    return store.read(
        dataType(),
        key,
        entry -> {
          Map<String, Object> values = asMap(entry);
          Map<String, T> result = new LinkedHashMap<>();
          for (String field : fields) {
            if (values.containsKey(field)) {
              result.put(field, TypeSafeValueCaster.cast(values.get(field), valueType));
            }
          }
          return result;
        },
        Map::of);
  }

  @Override
  public void putAll(String key, Map<String, T> values) {
    putAll(key, values, null);
  }

  @Override
  public void putAll(String key, Map<String, T> values, Duration ttl) {
    validator.validateKey(key);
    validator.validateMap(values);
    if (ttl != null) {
      validator.validateTtl(ttl);
    }
    for (Map.Entry<String, T> entry : values.entrySet()) {
      validator.validateField(entry.getKey());
      validator.validateValue(entry.getValue());
    }
    store.write(
        dataType(),
        key,
        (entry, now) -> {
          Map<String, Object> data = entry == null ? new ConcurrentHashMap<>() : asMap(entry);
          data.putAll(values);
          return LocalCacheWriteResult.keep(upsert(entry, data, now, ttl), null);
        });
  }

  @Override
  public boolean hasField(String key, String field) {
    validator.validateKey(key);
    validator.validateField(field);
    return store.read(dataType(), key, entry -> asMap(entry).containsKey(field), () -> false);
  }

  @Override
  public boolean delete(String key, String field) {
    validator.validateKey(key);
    validator.validateField(field);
    return store.write(
        dataType(),
        key,
        (entry, now) -> {
          if (entry == null) {
            return LocalCacheWriteResult.remove(false);
          }
          Map<String, Object> values = asMap(entry);
          boolean removed = values.remove(field) != null;
          if (values.isEmpty()) {
            return LocalCacheWriteResult.remove(removed);
          }
          return LocalCacheWriteResult.keep(entry.withValue(values, now), removed);
        });
  }

  @Override
  public long delete(String key, Collection<String> fields) {
    validator.validateKey(key);
    if (fields == null || fields.isEmpty()) {
      return 0L;
    }
    for (String field : fields) {
      validator.validateField(field);
    }
    return store.write(
        dataType(),
        key,
        (entry, now) -> {
          if (entry == null) {
            return LocalCacheWriteResult.remove(0L);
          }
          Map<String, Object> values = asMap(entry);
          long removed = 0L;
          for (String field : fields) {
            if (values.remove(field) != null) {
              removed++;
            }
          }
          if (values.isEmpty()) {
            return LocalCacheWriteResult.remove(removed);
          }
          return LocalCacheWriteResult.keep(entry.withValue(values, now), removed);
        });
  }

  @Override
  public Map<String, T> entries(String key) {
    validator.validateKey(key);
    return store.read(
        dataType(),
        key,
        entry -> {
          Map<String, T> result = new LinkedHashMap<>();
          for (Map.Entry<String, Object> mapEntry : asMap(entry).entrySet()) {
            result.put(mapEntry.getKey(), TypeSafeValueCaster.cast(mapEntry.getValue(), valueType));
          }
          return result;
        },
        Map::of);
  }

  @Override
  public long size(String key) {
    validator.validateKey(key);
    return store.read(dataType(), key, entry -> (long) asMap(entry).size(), () -> 0L);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> asMap(CacheEntry entry) {
    return (Map<String, Object>) entry.getValue();
  }

  private CacheEntry upsert(
      CacheEntry entry, Map<String, Object> values, Instant now, Duration ttl) {
    Instant expireAt = ttl == null ? (entry == null ? null : entry.getExpireAt()) : now.plus(ttl);
    if (entry == null) {
      return CacheEntry.create(dataType(), values, now, expireAt);
    }
    return new CacheEntry(dataType(), values, entry.getCreatedAt(), now, expireAt);
  }
}

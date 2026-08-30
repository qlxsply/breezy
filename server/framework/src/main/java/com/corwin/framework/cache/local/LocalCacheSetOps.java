package com.corwin.framework.cache.local;

import com.corwin.framework.cache.CacheDataType;
import com.corwin.framework.cache.CacheSetOps;
import com.corwin.framework.cache.core.CacheEntry;
import com.corwin.framework.cache.core.CacheKeyValidator;
import com.corwin.framework.cache.support.TypeSafeValueCaster;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local cache set operations backed by {@link java.util.concurrent.ConcurrentHashMap}.
 *
 * @author Corwin 2026/4/19
 */
public class LocalCacheSetOps<T> extends AbstractLocalCacheOps implements CacheSetOps<T> {

  private final Class<T> valueType;

  public LocalCacheSetOps(LocalCacheStore store, CacheKeyValidator validator, Class<T> valueType) {
    super(store, validator, CacheDataType.SET);
    this.valueType = validator.validateValueType(valueType);
  }

  @Override
  public long add(String key, T value) {
    validator.validateKey(key);
    validator.validateValue(value);
    return store.write(
        dataType(),
        key,
        (entry, now) -> {
          Set<Object> values = entry == null ? ConcurrentHashMap.newKeySet() : asSet(entry);
          boolean added = values.add(value);
          return LocalCacheWriteResult.keep(upsert(entry, values, now), added ? 1L : 0L);
        });
  }

  @Override
  public long addAll(String key, Collection<T> values) {
    validator.validateKey(key);
    validator.validateValues(values);
    return store.write(
        dataType(),
        key,
        (entry, now) -> {
          Set<Object> members = entry == null ? ConcurrentHashMap.newKeySet() : asSet(entry);
          long added = 0L;
          for (T value : values) {
            if (members.add(value)) {
              added++;
            }
          }
          return LocalCacheWriteResult.keep(upsert(entry, members, now), added);
        });
  }

  @Override
  public boolean remove(String key, T value) {
    validator.validateKey(key);
    validator.validateValue(value);
    return store.write(
        dataType(),
        key,
        (entry, now) -> {
          if (entry == null) {
            return LocalCacheWriteResult.remove(false);
          }
          Set<Object> members = asSet(entry);
          boolean removed = members.remove(value);
          if (members.isEmpty()) {
            return LocalCacheWriteResult.remove(removed);
          }
          return LocalCacheWriteResult.keep(entry.withValue(members, now), removed);
        });
  }

  @Override
  public long removeAll(String key, Collection<T> values) {
    validator.validateKey(key);
    validator.validateValues(values);
    return store.write(
        dataType(),
        key,
        (entry, now) -> {
          if (entry == null) {
            return LocalCacheWriteResult.remove(0L);
          }
          Set<Object> members = asSet(entry);
          long removed = 0L;
          for (T value : values) {
            if (members.remove(value)) {
              removed++;
            }
          }
          if (members.isEmpty()) {
            return LocalCacheWriteResult.remove(removed);
          }
          return LocalCacheWriteResult.keep(entry.withValue(members, now), removed);
        });
  }

  @Override
  public boolean isMember(String key, T value) {
    validator.validateKey(key);
    validator.validateValue(value);
    return store.read(dataType(), key, entry -> asSet(entry).contains(value), () -> false);
  }

  @Override
  public Set<T> members(String key) {
    validator.validateKey(key);
    return store.read(
        dataType(),
        key,
        entry -> {
          Set<T> result = new LinkedHashSet<>();
          for (Object value : asSet(entry)) {
            result.add(TypeSafeValueCaster.cast(value, valueType));
          }
          return result;
        },
        Set::of);
  }

  @Override
  public long size(String key) {
    validator.validateKey(key);
    return store.read(dataType(), key, entry -> (long) asSet(entry).size(), () -> 0L);
  }

  @SuppressWarnings("unchecked")
  private Set<Object> asSet(CacheEntry entry) {
    return (Set<Object>) entry.getValue();
  }

  private CacheEntry upsert(CacheEntry entry, Set<Object> values, Instant now) {
    if (entry == null) {
      return CacheEntry.create(dataType(), values, now, null);
    }
    return entry.withValue(values, now);
  }
}

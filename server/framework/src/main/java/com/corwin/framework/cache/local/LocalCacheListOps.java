package com.corwin.framework.cache.local;

import com.corwin.framework.cache.CacheDataType;
import com.corwin.framework.cache.CacheListOps;
import com.corwin.framework.cache.core.CacheEntry;
import com.corwin.framework.cache.core.CacheKeyValidator;
import com.corwin.framework.cache.support.TypeSafeValueCaster;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Local cache list operations backed by {@link LocalListValue}.
 *
 * @author Corwin 2026/4/19
 */
public class LocalCacheListOps<T> extends AbstractLocalCacheOps implements CacheListOps<T> {

  private final Class<T> valueType;

  public LocalCacheListOps(LocalCacheStore store, CacheKeyValidator validator, Class<T> valueType) {
    super(store, validator, CacheDataType.LIST);
    this.valueType = validator.validateValueType(valueType);
  }

  @Override
  public long leftPush(String key, T value) {
    validator.validateKey(key);
    validator.validateValue(value);
    return store.write(
        dataType(),
        key,
        (entry, now) -> {
          LocalListValue listValue = entry == null ? new LocalListValue() : asList(entry);
          long size = listValue.leftPush(value);
          return LocalCacheWriteResult.keep(upsert(entry, listValue, now), size);
        });
  }

  @Override
  public long rightPush(String key, T value) {
    validator.validateKey(key);
    validator.validateValue(value);
    return store.write(
        dataType(),
        key,
        (entry, now) -> {
          LocalListValue listValue = entry == null ? new LocalListValue() : asList(entry);
          long size = listValue.rightPush(value);
          return LocalCacheWriteResult.keep(upsert(entry, listValue, now), size);
        });
  }

  @Override
  public long leftPushAll(String key, Collection<T> values) {
    validator.validateKey(key);
    validator.validateValues(values);
    return store.write(
        dataType(),
        key,
        (entry, now) -> {
          LocalListValue listValue = entry == null ? new LocalListValue() : asList(entry);
          long size = listValue.leftPushAll(values);
          return LocalCacheWriteResult.keep(upsert(entry, listValue, now), size);
        });
  }

  @Override
  public long rightPushAll(String key, Collection<T> values) {
    validator.validateKey(key);
    validator.validateValues(values);
    return store.write(
        dataType(),
        key,
        (entry, now) -> {
          LocalListValue listValue = entry == null ? new LocalListValue() : asList(entry);
          long size = listValue.rightPushAll(values);
          return LocalCacheWriteResult.keep(upsert(entry, listValue, now), size);
        });
  }

  @Override
  public Optional<T> leftPop(String key) {
    validator.validateKey(key);
    return store.write(
        dataType(),
        key,
        (entry, now) -> {
          if (entry == null) {
            return LocalCacheWriteResult.remove(Optional.empty());
          }
          LocalListValue listValue = asList(entry);
          Optional<T> popped =
              listValue.leftPop().map(value -> TypeSafeValueCaster.cast(value, valueType));
          if (listValue.size() == 0L) {
            return LocalCacheWriteResult.remove(popped);
          }
          return LocalCacheWriteResult.keep(entry.withValue(listValue, now), popped);
        });
  }

  @Override
  public Optional<T> rightPop(String key) {
    validator.validateKey(key);
    return store.write(
        dataType(),
        key,
        (entry, now) -> {
          if (entry == null) {
            return LocalCacheWriteResult.remove(Optional.empty());
          }
          LocalListValue listValue = asList(entry);
          Optional<T> popped =
              listValue.rightPop().map(value -> TypeSafeValueCaster.cast(value, valueType));
          if (listValue.size() == 0L) {
            return LocalCacheWriteResult.remove(popped);
          }
          return LocalCacheWriteResult.keep(entry.withValue(listValue, now), popped);
        });
  }

  @Override
  public List<T> range(String key, long start, long end) {
    validator.validateKey(key);
    return store.read(
        dataType(), key, entry -> castList(asList(entry).range(start, end)), List::of);
  }

  @Override
  public void trim(String key, long start, long end) {
    validator.validateKey(key);
    store.write(
        dataType(),
        key,
        (entry, now) -> {
          if (entry == null) {
            return LocalCacheWriteResult.remove(null);
          }
          LocalListValue listValue = asList(entry);
          listValue.trim(start, end);
          if (listValue.size() == 0L) {
            return LocalCacheWriteResult.remove(null);
          }
          return LocalCacheWriteResult.keep(entry.withValue(listValue, now), null);
        });
  }

  @Override
  public long size(String key) {
    validator.validateKey(key);
    return store.read(dataType(), key, entry -> asList(entry).size(), () -> 0L);
  }

  private List<T> castList(List<Object> values) {
    List<T> result = new ArrayList<>(values.size());
    for (Object value : values) {
      result.add(TypeSafeValueCaster.cast(value, valueType));
    }
    return result;
  }

  private LocalListValue asList(CacheEntry entry) {
    return (LocalListValue) entry.getValue();
  }

  private CacheEntry upsert(CacheEntry entry, LocalListValue listValue, Instant now) {
    if (entry == null) {
      return CacheEntry.create(dataType(), listValue, now, null);
    }
    return entry.withValue(listValue, now);
  }
}

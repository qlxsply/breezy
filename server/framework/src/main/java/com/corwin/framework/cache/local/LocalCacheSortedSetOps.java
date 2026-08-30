package com.corwin.framework.cache.local;

import com.corwin.framework.cache.CacheDataType;
import com.corwin.framework.cache.CacheSortedSetOps;
import com.corwin.framework.cache.core.CacheEntry;
import com.corwin.framework.cache.core.CacheKeyValidator;
import com.corwin.framework.cache.support.TypeSafeValueCaster;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Local cache sorted-set operations backed by {@link LocalSortedSetValue}.
 *
 * @author Corwin 2026/4/19
 */
public class LocalCacheSortedSetOps<T> extends AbstractLocalCacheOps
    implements CacheSortedSetOps<T> {

  private final Class<T> valueType;

  public LocalCacheSortedSetOps(
      LocalCacheStore store, CacheKeyValidator validator, Class<T> valueType) {
    super(store, validator, CacheDataType.SORTED_SET);
    this.valueType = validator.validateValueType(valueType);
  }

  @Override
  public boolean add(String key, T member, double score) {
    validator.validateKey(key);
    validator.validateValue(member);
    return store.write(
        dataType(),
        key,
        (entry, now) -> {
          LocalSortedSetValue sortedSet =
              entry == null ? new LocalSortedSetValue() : asSortedSet(entry);
          boolean changed = sortedSet.add(member, score);
          return LocalCacheWriteResult.keep(upsert(entry, sortedSet, now), changed);
        });
  }

  @Override
  public long addAll(String key, Map<T, Double> members) {
    validator.validateKey(key);
    validator.validateMap(members);
    for (Map.Entry<T, Double> entry : members.entrySet()) {
      validator.validateValue(entry.getKey());
      validator.validateValue(entry.getValue());
    }
    return store.write(
        dataType(),
        key,
        (entry, now) -> {
          LocalSortedSetValue sortedSet =
              entry == null ? new LocalSortedSetValue() : asSortedSet(entry);
          long changed = sortedSet.addAll(members);
          return LocalCacheWriteResult.keep(upsert(entry, sortedSet, now), changed);
        });
  }

  @Override
  public boolean remove(String key, T member) {
    validator.validateKey(key);
    validator.validateValue(member);
    return store.write(
        dataType(),
        key,
        (entry, now) -> {
          if (entry == null) {
            return LocalCacheWriteResult.remove(false);
          }
          LocalSortedSetValue sortedSet = asSortedSet(entry);
          boolean removed = sortedSet.remove(member);
          if (sortedSet.size() == 0L) {
            return LocalCacheWriteResult.remove(removed);
          }
          return LocalCacheWriteResult.keep(entry.withValue(sortedSet, now), removed);
        });
  }

  @Override
  public long removeAll(String key, Collection<T> members) {
    validator.validateKey(key);
    validator.validateValues(members);
    return store.write(
        dataType(),
        key,
        (entry, now) -> {
          if (entry == null) {
            return LocalCacheWriteResult.remove(0L);
          }
          LocalSortedSetValue sortedSet = asSortedSet(entry);
          long removed = sortedSet.removeAll(members);
          if (sortedSet.size() == 0L) {
            return LocalCacheWriteResult.remove(removed);
          }
          return LocalCacheWriteResult.keep(entry.withValue(sortedSet, now), removed);
        });
  }

  @Override
  public Optional<Double> score(String key, T member) {
    validator.validateKey(key);
    validator.validateValue(member);
    return store.read(dataType(), key, entry -> asSortedSet(entry).score(member), Optional::empty);
  }

  @Override
  public Optional<Long> rank(String key, T member) {
    validator.validateKey(key);
    validator.validateValue(member);
    return store.read(dataType(), key, entry -> asSortedSet(entry).rank(member), Optional::empty);
  }

  @Override
  public Optional<Long> reverseRank(String key, T member) {
    validator.validateKey(key);
    validator.validateValue(member);
    return store.read(
        dataType(), key, entry -> asSortedSet(entry).reverseRank(member), Optional::empty);
  }

  @Override
  public List<T> range(String key, long start, long end) {
    validator.validateKey(key);
    return store.read(
        dataType(), key, entry -> castList(asSortedSet(entry).range(start, end)), List::of);
  }

  @Override
  public List<T> reverseRange(String key, long start, long end) {
    validator.validateKey(key);
    return store.read(
        dataType(), key, entry -> castList(asSortedSet(entry).reverseRange(start, end)), List::of);
  }

  @Override
  public List<T> rangeByScore(String key, double minScore, double maxScore) {
    validator.validateKey(key);
    return store.read(
        dataType(),
        key,
        entry -> castList(asSortedSet(entry).rangeByScore(minScore, maxScore)),
        List::of);
  }

  @Override
  public long countByScore(String key, double minScore, double maxScore) {
    validator.validateKey(key);
    return store.read(
        dataType(), key, entry -> asSortedSet(entry).countByScore(minScore, maxScore), () -> 0L);
  }

  @Override
  public long size(String key) {
    validator.validateKey(key);
    return store.read(dataType(), key, entry -> asSortedSet(entry).size(), () -> 0L);
  }

  private List<T> castList(List<Object> members) {
    List<T> result = new ArrayList<>(members.size());
    for (Object member : members) {
      result.add(TypeSafeValueCaster.cast(member, valueType));
    }
    return result;
  }

  private LocalSortedSetValue asSortedSet(CacheEntry entry) {
    return (LocalSortedSetValue) entry.getValue();
  }

  private CacheEntry upsert(CacheEntry entry, LocalSortedSetValue sortedSet, Instant now) {
    if (entry == null) {
      return CacheEntry.create(dataType(), sortedSet, now, null);
    }
    return entry.withValue(sortedSet, now);
  }
}

package com.corwin.framework.cache.local;

import com.corwin.framework.cache.CacheDataType;
import com.corwin.framework.cache.CacheTtlResult;
import com.corwin.framework.cache.config.FrameworkCacheProperties;
import com.corwin.framework.cache.core.CacheEntry;
import com.corwin.framework.error.BizException;
import com.corwin.framework.error.CacheError;
import com.corwin.framework.error.SysException;
import com.corwin.framework.util.HighDate;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * {@link LocalCacheStore} implementation backed by Caffeine.
 *
 * @author Corwin 2026/4/19
 */
public class CaffeineLocalCacheStore implements LocalCacheStore, InitializingBean, DisposableBean {

  private final FrameworkCacheProperties.Local properties;
  private final Cache<String, CacheEntry> cache;
  private ScheduledExecutorService cleanupExecutor;

  public CaffeineLocalCacheStore(FrameworkCacheProperties.Local properties) {
    this.properties = Objects.requireNonNull(properties, "properties required");
    Caffeine<Object, Object> builder = Caffeine.newBuilder();
    builder.expireAfter(new CacheEntryExpiry());
    if (properties.isRecordStats()) {
      builder.recordStats();
    }
    if (properties.getEviction().isEnabled()) {
      builder.maximumSize(properties.getEviction().getMaximumSize());
    }
    this.cache = builder.build();
  }

  @Override
  public <R> R read(
      CacheDataType dataType,
      String key,
      Function<CacheEntry, R> reader,
      Supplier<R> absentSupplier) {
    try {
      CacheEntry entry = cache.getIfPresent(key);
      if (entry == null) {
        return absentSupplier.get();
      }
      Instant now = HighDate.realInstant();
      if (entry.isExpired(now)) {
        cache.invalidate(key);
        return absentSupplier.get();
      }
      validateDataType(key, dataType, entry);
      return reader.apply(entry);
    } catch (BizException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new SysException(ex, CacheError.CACHE_LOCAL_STORE_ERROR);
    }
  }

  @Override
  public <R> R write(
      CacheDataType dataType,
      String key,
      BiFunction<CacheEntry, Instant, LocalCacheWriteResult<R>> writer) {
    try {
      AtomicReference<R> resultRef = new AtomicReference<>();
      cache
          .asMap()
          .compute(
              key,
              (ignored, existing) -> {
                Instant now = HighDate.realInstant();
                CacheEntry current = existing;
                if (current != null && current.isExpired(now)) {
                  current = null;
                }
                if (current != null) {
                  validateDataType(key, dataType, current);
                }
                LocalCacheWriteResult<R> result = writer.apply(current, now);
                resultRef.set(result.result());
                return result.remove() ? null : result.entry();
              });
      return resultRef.get();
    } catch (BizException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new SysException(ex, CacheError.CACHE_LOCAL_STORE_ERROR);
    }
  }

  @Override
  public boolean exists(CacheDataType dataType, String key) {
    return read(dataType, key, ignored -> true, () -> false);
  }

  @Override
  public boolean delete(CacheDataType dataType, String key) {
    return write(
        dataType,
        key,
        (entry, now) -> {
          if (entry == null) {
            return LocalCacheWriteResult.remove(false);
          }
          return LocalCacheWriteResult.remove(true);
        });
  }

  @Override
  public boolean expire(CacheDataType dataType, String key, Duration ttl) {
    return write(
        dataType,
        key,
        (entry, now) -> {
          if (entry == null) {
            return LocalCacheWriteResult.remove(false);
          }
          CacheEntry updated = entry.withExpireAt(now.plus(ttl), now);
          return LocalCacheWriteResult.keep(updated, true);
        });
  }

  @Override
  public CacheTtlResult ttl(CacheDataType dataType, String key) {
    return read(
        dataType,
        key,
        entry -> {
          if (entry.isPersistent()) {
            return CacheTtlResult.persistentValue();
          }
          Duration ttl = entry.remainingTtl(HighDate.realInstant());
          if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            cache.invalidate(key);
            return CacheTtlResult.notExists();
          }
          return CacheTtlResult.expiring(ttl);
        },
        CacheTtlResult::notExists);
  }

  @Override
  public void afterPropertiesSet() {
    if (!properties.getCleanup().isEnabled()) {
      return;
    }
    ThreadFactory threadFactory =
        runnable -> {
          Thread thread = new Thread(runnable, "framework-cache-cleanup");
          thread.setDaemon(true);
          return thread;
        };
    cleanupExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
    long intervalMillis = properties.getCleanup().getInterval().toMillis();
    cleanupExecutor.scheduleWithFixedDelay(
        cache::cleanUp, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
  }

  @Override
  public void destroy() {
    if (cleanupExecutor != null) {
      cleanupExecutor.shutdownNow();
    }
    cache.invalidateAll();
  }

  private void validateDataType(String key, CacheDataType expectedType, CacheEntry entry) {
    if (entry.getDataType() != expectedType) {
      CacheDataType dataType = entry.getDataType();
      String msg =
          String.format(
              "Cache key '%s' already exists as %s, expected %s", key, dataType, expectedType);
      throw new BizException(msg, CacheError.CACHE_DATA_TYPE_MISMATCH);
    }
  }

  private static final class CacheEntryExpiry implements Expiry<String, CacheEntry> {

    @Override
    public long expireAfterCreate(String key, CacheEntry value, long currentTime) {
      return expireAfter(value);
    }

    @Override
    public long expireAfterUpdate(
        String key, CacheEntry value, long currentTime, long currentDuration) {
      return expireAfter(value);
    }

    @Override
    public long expireAfterRead(
        String key, CacheEntry value, long currentTime, long currentDuration) {
      return currentDuration;
    }

    private long expireAfter(CacheEntry value) {
      if (value.isPersistent()) {
        return Long.MAX_VALUE;
      }
      Duration remaining = Duration.between(HighDate.realInstant(), value.getExpireAt());
      if (remaining.isZero() || remaining.isNegative()) {
        return 0L;
      }
      return Math.max(1L, remaining.toNanos());
    }
  }
}

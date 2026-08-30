package com.corwin.framework.cache;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Object-type cache operations.
 *
 * @author Corwin 2026/4/19
 */
public interface CacheObjectOps<T> extends CacheKeyOps {

  Optional<T> get(String key);

  void set(String key, T value);

  void set(String key, T value, Duration ttl);

  Map<String, T> multiGet(Collection<String> keys);

  void multiSet(Map<String, T> values);

  void multiSet(Map<String, T> values, Duration ttl);
}

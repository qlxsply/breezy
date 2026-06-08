package com.corwin.framework.cache;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * String 类型缓存操作。
 *
 * @author Corwin 2026/4/19
 */
public interface CacheStringOps extends CacheKeyOps {

    Optional<String> get(String key);

    void set(String key, String value);

    void set(String key, String value, Duration ttl);

    Map<String, String> multiGet(Collection<String> keys);

    void multiSet(Map<String, String> values);

    void multiSet(Map<String, String> values, Duration ttl);

    long increment(String key);

    long increment(String key, long delta);

    long decrement(String key);

    long decrement(String key, long delta);
}

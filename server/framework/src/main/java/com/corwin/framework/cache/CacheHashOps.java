package com.corwin.framework.cache;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Hash-type cache operations.
 *
 * @author Corwin 2026/4/19
 */
public interface CacheHashOps<T> extends CacheKeyOps {

    Optional<T> get(String key, String field);

    void put(String key, String field, T value);

    void put(String key, String field, T value, Duration ttl);

    Map<String, T> multiGet(String key, Collection<String> fields);

    void putAll(String key, Map<String, T> values);

    void putAll(String key, Map<String, T> values, Duration ttl);

    boolean hasField(String key, String field);

    boolean delete(String key, String field);

    long delete(String key, Collection<String> fields);

    Map<String, T> entries(String key);

    long size(String key);
}

package com.corwin.framework.cache;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * List-type cache operations.
 *
 * @author Corwin 2026/4/19
 */
public interface CacheListOps<T> extends CacheKeyOps {

    long leftPush(String key, T value);

    long rightPush(String key, T value);

    long leftPushAll(String key, Collection<T> values);

    long rightPushAll(String key, Collection<T> values);

    Optional<T> leftPop(String key);

    Optional<T> rightPop(String key);

    List<T> range(String key, long start, long end);

    void trim(String key, long start, long end);

    long size(String key);
}

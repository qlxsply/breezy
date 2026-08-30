package com.corwin.framework.cache;

import java.util.Collection;
import java.util.Set;

/**
 * Set-type cache operations.
 *
 * @author Corwin 2026/4/19
 */
public interface CacheSetOps<T> extends CacheKeyOps {

  long add(String key, T value);

  long addAll(String key, Collection<T> values);

  boolean remove(String key, T value);

  long removeAll(String key, Collection<T> values);

  boolean isMember(String key, T value);

  Set<T> members(String key);

  long size(String key);
}

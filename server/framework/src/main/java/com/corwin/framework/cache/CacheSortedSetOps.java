package com.corwin.framework.cache;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Sorted-set-type cache operations.
 *
 * @author Corwin 2026/4/19
 */
public interface CacheSortedSetOps<T> extends CacheKeyOps {

    boolean add(String key, T member, double score);

    long addAll(String key, Map<T, Double> members);

    boolean remove(String key, T member);

    long removeAll(String key, Collection<T> members);

    Optional<Double> score(String key, T member);

    Optional<Long> rank(String key, T member);

    Optional<Long> reverseRank(String key, T member);

    List<T> range(String key, long start, long end);

    List<T> reverseRange(String key, long start, long end);

    List<T> rangeByScore(String key, double minScore, double maxScore);

    long countByScore(String key, double minScore, double maxScore);

    long size(String key);
}

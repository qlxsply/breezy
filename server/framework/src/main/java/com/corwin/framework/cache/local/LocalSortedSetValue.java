package com.corwin.framework.cache.local;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe local sorted-set structure backed by a {@link java.util.TreeMap} and read-write lock.
 *
 * @author Corwin 2026/4/19
 */
public final class LocalSortedSetValue {

  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final Map<Object, Double> memberScores = new HashMap<>();
  private final NavigableMap<Double, LinkedHashSet<Object>> scoreMembers = new TreeMap<>();

  public boolean add(Object member, double score) {
    lock.writeLock().lock();
    try {
      Double existingScore = memberScores.get(member);
      if (existingScore != null && Double.compare(existingScore, score) == 0) {
        return false;
      }
      if (existingScore != null) {
        removeInternal(member, existingScore);
      }
      memberScores.put(member, score);
      scoreMembers.computeIfAbsent(score, ignored -> new LinkedHashSet<>()).add(member);
      return true;
    } finally {
      lock.writeLock().unlock();
    }
  }

  public long addAll(Map<?, Double> members) {
    lock.writeLock().lock();
    try {
      long changed = 0L;
      for (Map.Entry<?, Double> entry : members.entrySet()) {
        Object member = entry.getKey();
        Double score = entry.getValue();
        Double existingScore = memberScores.get(member);
        if (existingScore != null && Double.compare(existingScore, score) == 0) {
          continue;
        }
        if (existingScore != null) {
          removeInternal(member, existingScore);
        }
        memberScores.put(member, score);
        scoreMembers.computeIfAbsent(score, ignored -> new LinkedHashSet<>()).add(member);
        changed++;
      }
      return changed;
    } finally {
      lock.writeLock().unlock();
    }
  }

  public boolean remove(Object member) {
    lock.writeLock().lock();
    try {
      Double score = memberScores.remove(member);
      if (score == null) {
        return false;
      }
      LinkedHashSet<Object> members = scoreMembers.get(score);
      if (members != null) {
        members.remove(member);
        if (members.isEmpty()) {
          scoreMembers.remove(score);
        }
      }
      return true;
    } finally {
      lock.writeLock().unlock();
    }
  }

  public long removeAll(Collection<?> members) {
    lock.writeLock().lock();
    try {
      long removed = 0L;
      for (Object member : members) {
        Double score = memberScores.remove(member);
        if (score == null) {
          continue;
        }
        LinkedHashSet<Object> scoreSet = scoreMembers.get(score);
        if (scoreSet != null) {
          scoreSet.remove(member);
          if (scoreSet.isEmpty()) {
            scoreMembers.remove(score);
          }
        }
        removed++;
      }
      return removed;
    } finally {
      lock.writeLock().unlock();
    }
  }

  public Optional<Double> score(Object member) {
    lock.readLock().lock();
    try {
      return Optional.ofNullable(memberScores.get(member));
    } finally {
      lock.readLock().unlock();
    }
  }

  public Optional<Long> rank(Object member) {
    lock.readLock().lock();
    try {
      if (!memberScores.containsKey(member)) {
        return Optional.empty();
      }
      List<Object> ascending = snapshotAscending();
      for (int i = 0; i < ascending.size(); i++) {
        if (ascending.get(i).equals(member)) {
          return Optional.of((long) i);
        }
      }
      return Optional.empty();
    } finally {
      lock.readLock().unlock();
    }
  }

  public Optional<Long> reverseRank(Object member) {
    lock.readLock().lock();
    try {
      if (!memberScores.containsKey(member)) {
        return Optional.empty();
      }
      List<Object> descending = snapshotDescending();
      for (int i = 0; i < descending.size(); i++) {
        if (descending.get(i).equals(member)) {
          return Optional.of((long) i);
        }
      }
      return Optional.empty();
    } finally {
      lock.readLock().unlock();
    }
  }

  public List<Object> range(long start, long end) {
    lock.readLock().lock();
    try {
      List<Object> ascending = snapshotAscending();
      RangeIndex rangeIndex = RangeIndexSupport.normalize(start, end, ascending.size());
      if (rangeIndex == null) {
        return List.of();
      }
      return new ArrayList<>(ascending.subList(rangeIndex.start(), rangeIndex.end() + 1));
    } finally {
      lock.readLock().unlock();
    }
  }

  public List<Object> reverseRange(long start, long end) {
    lock.readLock().lock();
    try {
      List<Object> descending = snapshotDescending();
      RangeIndex rangeIndex = RangeIndexSupport.normalize(start, end, descending.size());
      if (rangeIndex == null) {
        return List.of();
      }
      return new ArrayList<>(descending.subList(rangeIndex.start(), rangeIndex.end() + 1));
    } finally {
      lock.readLock().unlock();
    }
  }

  public List<Object> rangeByScore(double minScore, double maxScore) {
    lock.readLock().lock();
    try {
      List<Object> result = new ArrayList<>();
      for (Map.Entry<Double, LinkedHashSet<Object>> entry :
          scoreMembers.subMap(minScore, true, maxScore, true).entrySet()) {
        result.addAll(entry.getValue());
      }
      return result;
    } finally {
      lock.readLock().unlock();
    }
  }

  public long countByScore(double minScore, double maxScore) {
    lock.readLock().lock();
    try {
      long count = 0L;
      for (LinkedHashSet<Object> members :
          scoreMembers.subMap(minScore, true, maxScore, true).values()) {
        count += members.size();
      }
      return count;
    } finally {
      lock.readLock().unlock();
    }
  }

  public long size() {
    lock.readLock().lock();
    try {
      return memberScores.size();
    } finally {
      lock.readLock().unlock();
    }
  }

  private void removeInternal(Object member, double score) {
    LinkedHashSet<Object> members = scoreMembers.get(score);
    if (members != null) {
      members.remove(member);
      if (members.isEmpty()) {
        scoreMembers.remove(score);
      }
    }
  }

  private List<Object> snapshotAscending() {
    List<Object> snapshot = new ArrayList<>(memberScores.size());
    for (LinkedHashSet<Object> members : scoreMembers.values()) {
      snapshot.addAll(members);
    }
    return snapshot;
  }

  private List<Object> snapshotDescending() {
    List<Object> snapshot = new ArrayList<>(memberScores.size());
    for (LinkedHashSet<Object> members : scoreMembers.descendingMap().values()) {
      snapshot.addAll(members);
    }
    return snapshot;
  }
}

package com.corwin.framework.cache.local;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe local list structure backed by an {@link java.util.ArrayDeque}.
 *
 * @author Corwin 2026/4/19
 */
public final class LocalListValue {

  private final ReentrantLock lock = new ReentrantLock();
  private final Deque<Object> values = new ArrayDeque<>();

  public long leftPush(Object value) {
    lock.lock();
    try {
      values.addFirst(value);
      return values.size();
    } finally {
      lock.unlock();
    }
  }

  public long rightPush(Object value) {
    lock.lock();
    try {
      values.addLast(value);
      return values.size();
    } finally {
      lock.unlock();
    }
  }

  public long leftPushAll(Collection<?> newValues) {
    lock.lock();
    try {
      for (Object value : newValues) {
        values.addFirst(value);
      }
      return values.size();
    } finally {
      lock.unlock();
    }
  }

  public long rightPushAll(Collection<?> newValues) {
    lock.lock();
    try {
      values.addAll(newValues);
      return values.size();
    } finally {
      lock.unlock();
    }
  }

  public Optional<Object> leftPop() {
    lock.lock();
    try {
      return Optional.ofNullable(values.pollFirst());
    } finally {
      lock.unlock();
    }
  }

  public Optional<Object> rightPop() {
    lock.lock();
    try {
      return Optional.ofNullable(values.pollLast());
    } finally {
      lock.unlock();
    }
  }

  public List<Object> range(long start, long end) {
    lock.lock();
    try {
      List<Object> snapshot = new ArrayList<>(values);
      RangeIndex rangeIndex = RangeIndexSupport.normalize(start, end, snapshot.size());
      if (rangeIndex == null) {
        return List.of();
      }
      return new ArrayList<>(snapshot.subList(rangeIndex.start(), rangeIndex.end() + 1));
    } finally {
      lock.unlock();
    }
  }

  public void trim(long start, long end) {
    lock.lock();
    try {
      List<Object> snapshot = new ArrayList<>(values);
      RangeIndex rangeIndex = RangeIndexSupport.normalize(start, end, snapshot.size());
      values.clear();
      if (rangeIndex == null) {
        return;
      }
      values.addAll(snapshot.subList(rangeIndex.start(), rangeIndex.end() + 1));
    } finally {
      lock.unlock();
    }
  }

  public long size() {
    lock.lock();
    try {
      return values.size();
    } finally {
      lock.unlock();
    }
  }
}

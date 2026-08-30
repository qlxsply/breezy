package com.corwin.framework.cache.local;

/**
 * Utility for normalizing Redis-style range indices (supporting negative offsets).
 *
 * @author Corwin 2026/4/19
 */
public abstract class RangeIndexSupport {

  private RangeIndexSupport() {}

  public static RangeIndex normalize(long start, long end, int size) {
    if (size <= 0) {
      return null;
    }
    int normalizedStart = normalizeIndex(start, size);
    int normalizedEnd = normalizeIndex(end, size);
    if (normalizedStart < 0) {
      normalizedStart = 0;
    }
    if (normalizedEnd < 0) {
      return null;
    }
    if (normalizedStart >= size) {
      return null;
    }
    if (normalizedEnd >= size) {
      normalizedEnd = size - 1;
    }
    if (normalizedStart > normalizedEnd) {
      return null;
    }
    return new RangeIndex(normalizedStart, normalizedEnd);
  }

  private static int normalizeIndex(long index, int size) {
    long normalized = index >= 0 ? index : size + index;
    if (normalized < Integer.MIN_VALUE) {
      return Integer.MIN_VALUE;
    }
    if (normalized > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    return (int) normalized;
  }
}

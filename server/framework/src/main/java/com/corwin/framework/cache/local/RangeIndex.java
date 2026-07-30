package com.corwin.framework.cache.local;

/**
 * A normalized range index with inclusive start and exclusive end positions.
 *
 * @author Corwin 2026/4/19
 */
public record RangeIndex(
        int start,
        int end
) {
}

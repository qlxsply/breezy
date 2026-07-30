package com.corwin.framework.web.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * Generic list response wrapper.
 * <p>
 * Carries the element list along with total and current count metadata.
 *
 * @param <T> the element type
 * @author Corwin 2026/1/8
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ListResult<T> {
    /**
     * Total number of elements (across all pages)
     */
    private final Integer count;
    /**
     * Number of elements in the current response
     */
    private final Integer size;
    /**
     * The current page's element list
     */
    private final List<T> list;

    public static <T> ListResult<T> of(List<T> list) {
        List<T> safe = (list == null) ? Collections.emptyList() : list;
        int n = safe.size();
        return new ListResult<>(n, n, safe);
    }

    public static <T> ListResult<T> of(List<T> list, long count) {
        List<T> safe = (list == null) ? Collections.emptyList() : list;
        int count0 = Math.max((int) count, 0);
        return new ListResult<>(count0, safe.size(), safe);
    }

}

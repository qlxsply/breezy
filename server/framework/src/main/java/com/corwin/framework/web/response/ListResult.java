package com.corwin.framework.web.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 *
 * @author Corwin 2026/1/8
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ListResult<T> {
    /**
     * 元素总数量
     */
    private final Integer count;
    /**
     * 当前元素数量
     */
    private final Integer size;
    /**
     * 当前元素列表
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

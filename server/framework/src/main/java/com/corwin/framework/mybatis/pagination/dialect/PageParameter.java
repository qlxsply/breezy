package com.corwin.framework.mybatis.pagination.dialect;

/**
 * 分页 SQL 追加参数。
 *
 * @author Corwin 2026/7/28
 */
public record PageParameter(
        String name,
        Class<?> javaType,
        Object value
) {
}

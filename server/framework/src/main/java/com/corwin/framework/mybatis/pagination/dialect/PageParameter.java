package com.corwin.framework.mybatis.pagination.dialect;

/**
 * A single parameter appended to a paginated SQL statement,
 * carrying its name, Java type, and runtime value.
 *
 * @param name     the parameter name
 * @param javaType the parameter Java type
 * @param value    the parameter value
 * @author Corwin 2026/7/28
 */
public record PageParameter(
        String name,
        Class<?> javaType,
        Object value
) {
}

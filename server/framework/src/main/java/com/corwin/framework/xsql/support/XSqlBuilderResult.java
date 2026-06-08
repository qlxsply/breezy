package com.corwin.framework.xsql.support;

import java.util.List;

/**
 * SQL 构建结果。
 * <p>
 * 封装动态拼接后的 SQL 与对应参数列表。
 *
 * @author Corwin 2026/4/9
 */
public record XSqlBuilderResult(
        String sql,
        List<Object> params
) {
}


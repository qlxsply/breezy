package com.corwin.framework.xsql.support;

import com.corwin.framework.xsql.XSortDirection;

/**
 * XSql 排序对象。
 * <p>
 * 表示单个 {@code ORDER BY} 项。
 *
 * @author Corwin 2026/4/9
 */
public record XSqlOrder(
        String fieldExpr,
        XSortDirection direction
) {
}


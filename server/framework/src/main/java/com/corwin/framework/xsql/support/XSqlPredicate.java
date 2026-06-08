package com.corwin.framework.xsql.support;

import java.util.List;

/**
 * XSql 条件对象。
 * <p>
 * 表示单个 where 谓词表达式。
 *
 * @author Corwin 2026/4/9
 */
public record XSqlPredicate(
        String fieldExpr,
        XSqlOperator operator,
        List<Object> values
) {
}


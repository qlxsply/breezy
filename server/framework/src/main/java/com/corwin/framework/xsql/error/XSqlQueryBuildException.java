package com.corwin.framework.xsql.error;

import com.corwin.framework.error.XSqlError;

/**
 * XSql 查询构建异常。
 * <p>
 * 发生在 DSL 构建阶段，如属性路径非法、排序字段越界、分页参数不合法等。
 *
 * @author Corwin 2026/4/9
 */
public class XSqlQueryBuildException extends XSqlException {

    /**
     * 构造查询构建异常。
     */
    public XSqlQueryBuildException(String message) {
        super(message, XSqlError.XSQL_QUERY_BUILD_ERROR);
    }
}


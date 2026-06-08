package com.corwin.framework.xsql.error;

import com.corwin.framework.error.XSqlError;

/**
 * XSql one 查询返回多条异常。
 * <p>
 * {@code one()} 语义要求最多一条记录；超出时抛出该异常。
 *
 * @author Corwin 2026/4/9
 */
public class XSqlTooManyResultsException extends XSqlException {

    /**
     * 构造多结果异常。
     */
    public XSqlTooManyResultsException(String message) {
        super(message, XSqlError.XSQL_TOO_MANY_RESULTS);
    }
}


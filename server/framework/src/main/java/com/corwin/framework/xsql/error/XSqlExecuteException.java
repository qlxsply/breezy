package com.corwin.framework.xsql.error;

import com.corwin.framework.error.XSqlError;

/**
 * XSql SQL 执行异常。
 * <p>
 * 用于 JDBC 执行阶段异常包装。
 *
 * @author Corwin 2026/4/9
 */
public class XSqlExecuteException extends XSqlException {

    /**
     * 构造执行异常。
     */
    public XSqlExecuteException(String message) {
        super(message, XSqlError.XSQL_SQL_EXECUTE_ERROR);
    }

    /**
     * 构造带原因的执行异常。
     */
    public XSqlExecuteException(String message, Throwable cause) {
        super(message + ", cause: " + (cause == null ? "unknown" : cause.getMessage()), XSqlError.XSQL_SQL_EXECUTE_ERROR);
    }
}


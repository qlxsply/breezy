package com.corwin.framework.xsql.error;

import com.corwin.framework.error.XSqlError;

/**
 * XSql 方言异常。
 * <p>
 * 用于数据库类型不支持或方言识别失败场景。
 *
 * @author Corwin 2026/4/9
 */
public class XSqlDialectException extends XSqlException {

    /**
     * 构造“数据库不支持”异常。
     */
    public XSqlDialectException(String message) {
        super(message, XSqlError.XSQL_UNSUPPORTED_DATABASE);
    }

    /**
     * 构造“数据源识别失败”异常。
     */
    public XSqlDialectException(String message, Throwable cause) {
        super(message + ", cause: " + (cause == null ? "unknown" : cause.getMessage()),
                XSqlError.XSQL_DATASOURCE_ERROR);
    }
}


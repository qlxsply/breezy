package com.corwin.framework.xsql.error;

import com.corwin.framework.error.XSqlError;

/**
 * XSql 命名策略解析异常。
 * <p>
 * 用于表示当前运行环境无法为 XSql 提供与 JPA Provider 一致的物理命名能力。
 *
 * @author Corwin 2026/4/17
 */
public class XSqlNamingStrategyException extends XSqlException {

    /**
     * 使用消息构造异常。
     */
    public XSqlNamingStrategyException(String message) {
        super(message, XSqlError.XSQL_NAMING_STRATEGY_ERROR);
    }

    /**
     * 使用消息与原因构造异常。
     */
    public XSqlNamingStrategyException(String message, Throwable cause) {
        super(message, cause, XSqlError.XSQL_NAMING_STRATEGY_ERROR);
    }
}

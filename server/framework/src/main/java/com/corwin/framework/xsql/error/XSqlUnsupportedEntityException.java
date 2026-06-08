package com.corwin.framework.xsql.error;

import com.corwin.framework.error.XSqlError;

/**
 * XSql 不支持的实体结构异常。
 * <p>
 * 用于明确告知某实体结构超出单表模式支持范围（如关系映射、secondary table 等）。
 *
 * @author Corwin 2026/4/9
 */
public class XSqlUnsupportedEntityException extends XSqlException {

    /**
     * 构造不支持实体异常。
     */
    public XSqlUnsupportedEntityException(String message) {
        super(message, XSqlError.XSQL_UNSUPPORTED_ENTITY);
    }
}


package com.corwin.framework.xsql.error;

import com.corwin.framework.error.XSqlError;

/**
 * XSql 元数据解析异常。
 * <p>
 * 发生在单表元数据解析阶段，通常属于实体结构不满足约束或命名解析失败。
 *
 * @author Corwin 2026/4/9
 */
public class XSqlMetaParseException extends XSqlException {

    /**
     * 构造元数据解析异常。
     */
    public XSqlMetaParseException(String message) {
        super(message, XSqlError.XSQL_META_PARSE_FAILED);
    }
}


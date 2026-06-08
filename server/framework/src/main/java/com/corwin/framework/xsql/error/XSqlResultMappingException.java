package com.corwin.framework.xsql.error;

import com.corwin.framework.error.XSqlError;

/**
 * XSql 结果映射异常。
 * <p>
 * 发生在 ResultSet 到 DTO/POJO 映射过程中，如 alias 不匹配、类型转换失败等。
 *
 * @author Corwin 2026/4/9
 */
public class XSqlResultMappingException extends XSqlException {

    /**
     * 构造结果映射异常。
     */
    public XSqlResultMappingException(String message) {
        super(message, XSqlError.XSQL_RESULT_MAPPING_ERROR);
    }
}


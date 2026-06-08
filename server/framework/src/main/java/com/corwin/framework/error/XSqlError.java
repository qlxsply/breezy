package com.corwin.framework.error;

/**
 * XSql 组件错误码定义。
 *
 * @author Corwin 2026/4/9
 */
public enum XSqlError implements ErrorCode {
    XSQL_UNSUPPORTED_DATABASE("010000", "Unsupported database type, only MySQL is supported"),
    XSQL_DATASOURCE_ERROR("010001", "Failed to access datasource metadata"),
    XSQL_META_PARSE_FAILED("010100", "Failed to parse entity metadata"),
    XSQL_UNSUPPORTED_ENTITY("010101", "Unsupported entity structure for single-table query"),
    XSQL_NAMING_STRATEGY_ERROR("010102", "Failed to resolve JPA physical naming strategy"),
    XSQL_QUERY_BUILD_ERROR("010200", "Failed to build SQL query"),
    XSQL_INVALID_PROPERTY("010201", "Invalid property path"),
    XSQL_INVALID_SORT("010202", "Invalid sort field"),
    XSQL_INVALID_PAGINATION("010203", "Invalid pagination argument"),
    XSQL_SQL_EXECUTE_ERROR("010300", "Failed to execute SQL query"),
    XSQL_RESULT_MAPPING_ERROR("010400", "Failed to map SQL result"),
    XSQL_TOO_MANY_RESULTS("010401", "Expected one result but found multiple rows"),
    ;

    private final String code;
    private final String msg;

    XSqlError(String code, String msg) {
        ErrorCodes.validate(code, msg, ErrorCodeRanges.FRAMEWORK_XSQL);
        this.code = code;
        this.msg = msg;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }

    @Override
    public ErrorCodeRange getRange() {
        return ErrorCodeRanges.FRAMEWORK_XSQL;
    }
}

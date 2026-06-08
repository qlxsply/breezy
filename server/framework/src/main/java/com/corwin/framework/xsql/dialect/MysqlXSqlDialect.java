package com.corwin.framework.xsql.dialect;

/**
 * XSql MySQL 方言。
 * <p>
 * 当前 v1 仅支持 MySQL 语法，分页采用 {@code limit offset, size} 模式。
 *
 * @author Corwin 2026/4/9
 */
public class MysqlXSqlDialect implements XSqlDialect {

    /**
     * 返回数据库类型。
     */
    @Override
    public DatabaseType type() {
        return DatabaseType.MYSQL;
    }

    /**
     * 使用反引号包裹标识符。
     */
    @Override
    public String quote(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return identifier;
        }
        return "`" + identifier + "`";
    }

    /**
     * 构建分页 SQL。
     */
    @Override
    public String buildPageSql(String sql) {
        return sql + " limit ?, ?";
    }

    /**
     * 构建 count 包装 SQL。
     */
    @Override
    public String buildCountWrapperSql(String sql) {
        return "select count(1) from (" + sql + ") x_count";
    }
}


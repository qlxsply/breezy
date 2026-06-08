package com.corwin.framework.xsql.dialect;

/**
 * XSql 方言抽象。
 * <p>
 * 负责封装数据库差异相关能力，当前聚焦：
 * 标识符引用、分页 SQL、统计包装 SQL。
 *
 * @author Corwin 2026/4/9
 */
public interface XSqlDialect {

    /**
     * 返回方言对应的数据库类型标识。
     */
    DatabaseType type();

    /**
     * 对标识符进行方言级引用（如 MySQL 使用反引号）。
     */
    String quote(String identifier);

    /**
     * 构建分页 SQL。
     *
     * @param sql 原始查询 SQL（通常已拼接 where/order）
     * @return 带分页参数占位符的 SQL
     */
    String buildPageSql(String sql);

    /**
     * 构建通用统计包装 SQL。
     *
     * @param sql 原始查询 SQL
     * @return 包装后的 count SQL
     */
    String buildCountWrapperSql(String sql);
}


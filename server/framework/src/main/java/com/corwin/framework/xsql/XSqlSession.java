package com.corwin.framework.xsql;

/**
 * XSql 会话对象。
 * <p>
 * 会话与数据源绑定，用于发起单表查询和原生 SQL 查询。
 * 每次调用 {@link XSql#using(javax.sql.DataSource)} 会创建新的会话。
 *
 * @author Corwin 2026/4/9
 */
public interface XSqlSession {

    /**
     * 创建单表查询 DSL。
     *
     * @param entityClass 实体类型（用于解析元数据）
     * @param resultClass 结果类型
     */
    <E, R> XTableQuery<E, R> table(Class<E> entityClass, Class<R> resultClass);

    /**
     * 创建原生 SQL 查询 DSL。
     *
     * @param resultClass 结果类型
     */
    <R> XNativeQuery<R> nativeQuery(Class<R> resultClass);
}


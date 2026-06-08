package com.corwin.framework.xsql.support;

/**
 * XSql 条件操作符。
 * <p>
 * 定义 DSL 支持的谓词操作集合，由 {@link XSqlWhereBuilder} 负责转成具体 SQL 片段。
 *
 * @author Corwin 2026/4/9
 */
public enum XSqlOperator {

    /**
     * 等于（=）。
     */
    EQ,
    /**
     * 不等于（<>）。
     */
    NE,
    /**
     * 大于（>）。
     */
    GT,
    /**
     * 小于（<）。
     */
    LT,
    /**
     * 大于等于（>=）。
     */
    GE,
    /**
     * 小于等于（<=）。
     */
    LE,
    /**
     * 在闭区间内（BETWEEN ... AND ...）。
     */
    BETWEEN,
    /**
     * 不在闭区间内（NOT BETWEEN ... AND ...）。
     */
    NOT_BETWEEN,
    /**
     * 在集合中（IN (...)）。
     */
    IN,
    /**
     * 不在集合中（NOT IN (...)）。
     */
    NOT_IN,
    /**
     * 双侧模糊（LIKE '%xxx%'）。
     */
    LIKE,
    /**
     * 左模糊（LIKE '%xxx'）。
     */
    LEFT_LIKE,
    /**
     * 右模糊（LIKE 'xxx%'）。
     */
    RIGHT_LIKE,
    /**
     * 为空（IS NULL）。
     */
    IS_NULL,
    /**
     * 不为空（IS NOT NULL）。
     */
    IS_NOT_NULL,
}


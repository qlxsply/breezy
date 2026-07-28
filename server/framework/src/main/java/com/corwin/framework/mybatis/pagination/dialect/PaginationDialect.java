package com.corwin.framework.mybatis.pagination.dialect;

/**
 * MyBatis 分页方言。
 *
 * @author Corwin 2026/7/28
 */
public interface PaginationDialect {

    String name();

    PageSql buildPageSql(String originalSql, long offset, int pageSize);
}

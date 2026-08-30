package com.corwin.framework.mybatis.pagination.dialect;

/**
 * Database-specific pagination dialect for MyBatis.
 *
 * <p>Implementations generate the database-specific SQL fragment (e.g. {@code LIMIT ... OFFSET
 * ...}) required to paginate a query.
 *
 * @author Corwin 2026/7/28
 */
public interface PaginationDialect {

  String name();

  PageSql buildPageSql(String originalSql, long offset, int pageSize);
}

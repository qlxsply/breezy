package com.corwin.framework.mybatis.pagination.dialect;

import java.util.List;

/**
 * {@link PaginationDialect} for MySQL and MariaDB.
 *
 * <p>Appends {@code LIMIT ? OFFSET ?} to the original SQL statement.
 *
 * @author Corwin 2026/7/28
 */
public final class MySqlPaginationDialect implements PaginationDialect {

  public static final String LIMIT_PARAMETER = "__corwin_page_limit";

  public static final String OFFSET_PARAMETER = "__corwin_page_offset";

  @Override
  public String name() {
    return "mysql";
  }

  @Override
  public PageSql buildPageSql(String originalSql, long offset, int pageSize) {
    String sql = trimTrailingSemicolon(originalSql);

    return new PageSql(
        sql + " LIMIT ? OFFSET ?",
        List.of(
            new PageParameter(LIMIT_PARAMETER, Integer.class, pageSize),
            new PageParameter(OFFSET_PARAMETER, Long.class, offset)));
  }

  private String trimTrailingSemicolon(String sql) {
    String result = sql == null ? "" : sql.trim();

    while (result.endsWith(";")) {
      result = result.substring(0, result.length() - 1).trim();
    }

    return result;
  }
}

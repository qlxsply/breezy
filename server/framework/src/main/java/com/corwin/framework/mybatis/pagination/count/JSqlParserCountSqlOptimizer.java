package com.corwin.framework.mybatis.pagination.count;

import com.corwin.framework.mybatis.pagination.PaginationException;
import com.corwin.framework.mybatis.pagination.SqlPlaceholderCounter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;

/**
 * Optimizes count SQL generation using JSqlParser-based AST analysis.
 *
 * <p>When safe, removes ORDER BY and replaces select items with {@code COUNT(*)} to allow the
 * database to use a simpler execution plan. Falls back to {@code SELECT COUNT(*) FROM (...)}
 * wrapping when the SQL cannot be safely rewritten.
 *
 * @author Corwin 2026/7/28
 */
public final class JSqlParserCountSqlOptimizer {

  private static final String COUNT_WRAPPER_ALIAS = "__corwin_page_count";

  private final int cacheSize;

  private final Map<CacheKey, String> cache;

  public JSqlParserCountSqlOptimizer(int cacheSize) {
    this.cacheSize = Math.max(cacheSize, 0);

    this.cache =
        new LinkedHashMap<>(64, 0.75F, true) {
          @Override
          protected boolean removeEldestEntry(Map.Entry<CacheKey, String> eldest) {
            return JSqlParserCountSqlOptimizer.this.cacheSize > 0
                && size() > JSqlParserCountSqlOptimizer.this.cacheSize;
          }
        };
  }

  public String buildCountSql(String originalSql, int parameterMappingCount) {
    String sql = trimTrailingSemicolon(originalSql);
    CacheKey key = new CacheKey(sql, parameterMappingCount);

    if (cacheSize > 0) {
      synchronized (cache) {
        String cached = cache.get(key);
        if (cached != null) {
          return cached;
        }
      }
    }

    String countSql = doBuildCountSql(sql, parameterMappingCount);

    if (cacheSize > 0) {
      synchronized (cache) {
        cache.put(key, countSql);
      }
    }

    return countSql;
  }

  private String doBuildCountSql(String originalSql, int parameterMappingCount) {
    /*
     * When the number of MyBatis ParameterMappings doesn't match the SQL ? count,
     * skip AST rewriting and fall back to conservative wrapping.
     */
    if (SqlPlaceholderCounter.count(originalSql) != parameterMappingCount) {
      return wrapCount(originalSql);
    }

    try {
      Statement statement = CCJSqlParserUtil.parse(originalSql);

      if (!(statement instanceof Select select)) {
        return wrapCount(originalSql);
      }

      rejectExistingPagination(select);

      OrderByRemoval orderByRemoval =
          removeTopLevelOrderBySafely(select, originalSql, parameterMappingCount);

      if (select instanceof PlainSelect plainSelect
          && orderByRemoval.directCountAllowed()
          && isSimpleSelect(plainSelect)) {

        replaceSelectItemsWithCount(plainSelect);

        String directCountSql = select.toString();

        if (SqlPlaceholderCounter.count(directCountSql) == parameterMappingCount) {
          return directCountSql;
        }
      }

      return wrapCount(orderByRemoval.sql());
    } catch (JSQLParserException exception) {
      /*
       * MySQL-specific syntax may not be parseable by JSqlParser.
       * Fall back to conservative wrapping when parsing fails.
       */
      return wrapCount(originalSql);
    }
  }

  private OrderByRemoval removeTopLevelOrderBySafely(
      Select select, String originalSql, int parameterMappingCount) {
    List<OrderByElement> originalOrderBy = select.getOrderByElements();

    if (originalOrderBy == null || originalOrderBy.isEmpty()) {
      return new OrderByRemoval(originalSql, true);
    }

    select.setOrderByElements(null);

    String candidate = select.toString();

    if (SqlPlaceholderCounter.count(candidate) == parameterMappingCount) {
      return new OrderByRemoval(candidate, true);
    }

    /*
     * ORDER BY contains JDBC parameters; restore it and skip direct COUNT rewriting.
     */
    select.setOrderByElements(originalOrderBy);

    return new OrderByRemoval(originalSql, false);
  }

  private boolean isSimpleSelect(PlainSelect select) {
    return select.getDistinct() == null
        && select.getGroupBy() == null
        && select.getHaving() == null
        && isEmpty(select.getIntoTables())
        && select.getIntoTempTable() == null
        && select.getQualify() == null
        && isEmpty(select.getWindowDefinitions())
        && !select.getMySqlSqlCalcFoundRows();
  }

  private void replaceSelectItemsWithCount(PlainSelect plainSelect) throws JSQLParserException {

    /*
     * Let JSqlParser parse COUNT(*) itself to avoid version-dependent AST differences.
     */
    PlainSelect countTemplate = (PlainSelect) CCJSqlParserUtil.parse("SELECT COUNT(*)");

    plainSelect.setSelectItems(countTemplate.getSelectItems());
  }

  private void rejectExistingPagination(Select select) {
    if (select.getLimit() != null || select.getOffset() != null || select.getFetch() != null) {
      throw new PaginationException(
          "Automatic pagination SQL must not contain "
              + "a top-level LIMIT, OFFSET or FETCH clause");
    }

    if (select instanceof PlainSelect plainSelect
        && (plainSelect.getTop() != null
            || plainSelect.getFirst() != null
            || plainSelect.getSkip() != null)) {
      throw new PaginationException(
          "Automatic pagination SQL already contains " + "a database pagination clause");
    }

    /*
     * The current MySQL implementation appends LIMIT at the end of SQL.
     * Locking queries would need dialect-specific AST rewriting; reject for now.
     */
    if (select.getForClause() != null
        || select.getForMode() != null
        || select.getForUpdateTable() != null) {
      throw new PaginationException(
          "Automatic pagination does not support " + "FOR UPDATE or locking clauses");
    }
  }

  private boolean isEmpty(List<?> list) {
    return list == null || list.isEmpty();
  }

  private String wrapCount(String sql) {
    return "SELECT COUNT(*) FROM (" + sql + ") " + COUNT_WRAPPER_ALIAS;
  }

  private String trimTrailingSemicolon(String sql) {
    String result = sql == null ? "" : sql.trim();

    while (result.endsWith(";")) {
      result = result.substring(0, result.length() - 1).trim();
    }

    return result;
  }

  private record CacheKey(String sql, int parameterCount) {}

  private record OrderByRemoval(String sql, boolean directCountAllowed) {}
}

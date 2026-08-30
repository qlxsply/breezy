package com.corwin.framework.mybatis.pagination;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.mybatis.pagination.count.JSqlParserCountSqlOptimizer;
import com.corwin.framework.mybatis.pagination.dialect.PageSql;
import com.corwin.framework.mybatis.pagination.dialect.PaginationDialect;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * MyBatis {@link org.apache.ibatis.plugin.Interceptor} that automatically rewrites SELECT queries
 * returning {@link PageData} and accepting {@link com.corwin.framework.domain.page.PageSpec
 * PageSpec} into paginated queries (count + limit/offset).
 *
 * @author Corwin 2026/7/28
 */
@Order(Ordered.LOWEST_PRECEDENCE)
@Intercepts({
  @Signature(
      type = Executor.class,
      method = "query",
      args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
  @Signature(
      type = Executor.class,
      method = "query",
      args = {
        MappedStatement.class,
        Object.class,
        RowBounds.class,
        ResultHandler.class,
        CacheKey.class,
        BoundSql.class
      })
})
public final class PageDataPaginationInterceptor implements Interceptor {

  private static final String COUNT_SUFFIX = "!corwin-page-count";

  private static final String PAGE_SUFFIX = "!corwin-page-query";

  private final PageMethodResolver methodResolver;

  private final PaginationDialect dialect;

  private final JSqlParserCountSqlOptimizer countSqlOptimizer;

  private final MybatisPaginationProperties properties;

  public PageDataPaginationInterceptor(
      PageMethodResolver methodResolver,
      PaginationDialect dialect,
      JSqlParserCountSqlOptimizer countSqlOptimizer,
      MybatisPaginationProperties properties) {
    this.methodResolver = methodResolver;
    this.dialect = dialect;
    this.countSqlOptimizer = countSqlOptimizer;
    this.properties = properties;
  }

  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    if (!properties.isEnabled()) {
      return invocation.proceed();
    }

    Object[] args = invocation.getArgs();

    MappedStatement mappedStatement = (MappedStatement) args[0];

    Object parameterObject = args[1];

    ResultHandler<?> resultHandler = (ResultHandler<?>) args[3];

    if (mappedStatement.getSqlCommandType() != SqlCommandType.SELECT) {
      return invocation.proceed();
    }

    /*
     * Queries with a custom ResultHandler are not eligible for automatic pagination.
     */
    if (resultHandler != null) {
      return invocation.proceed();
    }

    PageMethodDescriptor descriptor = methodResolver.resolve(mappedStatement.getId());

    if (!descriptor.automaticPagination()) {
      return invocation.proceed();
    }

    PageSpec pageSpec = resolvePageSpec(parameterObject);

    validatePageSpec(mappedStatement.getId(), pageSpec);

    long offset = calculateOffset(pageSpec);

    BoundSql originalBoundSql =
        args.length == 6 ? (BoundSql) args[5] : mappedStatement.getBoundSql(parameterObject);

    validateResultMapping(mappedStatement);

    Configuration configuration = mappedStatement.getConfiguration();

    Executor executor = (Executor) invocation.getTarget();

    String countSql =
        countSqlOptimizer.buildCountSql(
            originalBoundSql.getSql(), originalBoundSql.getParameterMappings().size());

    long total =
        executeCountQuery(
            executor, mappedStatement, parameterObject, originalBoundSql, countSql, configuration);

    /*
     * Short-circuit: no data to fetch, return an empty page immediately.
     */
    if (total == 0L || offset >= total) {
      PageData<Object> emptyPage = PageData.of(pageSpec, total, List.of());
      return List.of(emptyPage);
    }

    PageSql pageSql = dialect.buildPageSql(originalBoundSql.getSql(), offset, pageSpec.pageSize());

    BoundSql pageBoundSql = BoundSqlFactory.forPage(configuration, originalBoundSql, pageSql);

    MappedStatement pageMappedStatement =
        MappedStatementFactory.copy(
            mappedStatement,
            mappedStatement.getId() + PAGE_SUFFIX,
            new FixedBoundSqlSource(pageBoundSql),
            mappedStatement.getResultMaps());

    /*
     * Replace the current invocation arguments with the paginated query equivalents.
     */
    args[0] = pageMappedStatement;
    args[2] = RowBounds.DEFAULT;

    if (args.length == 6) {
      CacheKey pageCacheKey =
          executor.createCacheKey(
              pageMappedStatement, parameterObject, RowBounds.DEFAULT, pageBoundSql);

      args[4] = pageCacheKey;
      args[5] = pageBoundSql;
    }

    /*
     * MyBatis maps the result to List&lt;T&gt; per the XML resultMap/resultType.
     */
    @SuppressWarnings("unchecked")
    List<Object> elements = (List<Object>) invocation.proceed();

    PageData<Object> pageData = PageData.of(pageSpec, total, elements);

    /*
     * The MapperMethod ultimately calls selectOne(), which requires exactly one result.
     */
    return List.of(pageData);
  }

  private long executeCountQuery(
      Executor executor,
      MappedStatement originalStatement,
      Object parameterObject,
      BoundSql originalBoundSql,
      String countSql,
      Configuration configuration)
      throws Exception {

    BoundSql countBoundSql = BoundSqlFactory.forCount(configuration, originalBoundSql, countSql);

    String countStatementId = originalStatement.getId() + COUNT_SUFFIX;

    ResultMap countResultMap =
        new ResultMap.Builder(
                configuration, countStatementId + "!result-map", Long.class, List.of())
            .build();

    MappedStatement countStatement =
        MappedStatementFactory.copy(
            originalStatement,
            countStatementId,
            new FixedBoundSqlSource(countBoundSql),
            List.of(countResultMap));

    CacheKey countCacheKey =
        executor.createCacheKey(countStatement, parameterObject, RowBounds.DEFAULT, countBoundSql);

    /*
     * invocation.getTarget() is the Executor inside the plugin proxy.
     * Calling it directly does not re-enter this interceptor,
     * so no ThreadLocal recursion guard is needed.
     */
    List<?> countResult =
        executor.query(
            countStatement,
            parameterObject,
            RowBounds.DEFAULT,
            Executor.NO_RESULT_HANDLER,
            countCacheKey,
            countBoundSql);

    if (countResult == null || countResult.isEmpty() || countResult.getFirst() == null) {
      return 0L;
    }

    if (countResult.size() != 1) {
      throw new PaginationException(
          "Count query returned "
              + countResult.size()
              + " rows for statement "
              + originalStatement.getId());
    }

    Object value = countResult.getFirst();

    if (value instanceof Number number) {
      return number.longValue();
    }

    try {
      return Long.parseLong(value.toString());
    } catch (NumberFormatException exception) {
      throw new PaginationException(
          "Count query did not return a numeric value "
              + "for statement "
              + originalStatement.getId()
              + ": "
              + value,
          exception);
    }
  }

  private PageSpec resolvePageSpec(Object parameterObject) {
    if (parameterObject instanceof PageSpec pageSpec) {
      return pageSpec;
    }

    /*
     * Multi-parameter mapper methods are wrapped in a ParamMap by MyBatis.
     * The same PageSpec may appear under multiple keys (e.g. "page" and "param1").
     * Pick the first one found.
     */
    if (parameterObject instanceof Map<?, ?> map) {
      for (Object value : map.values()) {
        if (value instanceof PageSpec pageSpec) {
          return pageSpec;
        }
      }
    }

    /*
     * The method signature confirmed a PageSpec parameter exists.
     * If the runtime value is missing, fall back to defaults.
     */
    return PageSpec.of(null, null, null);
  }

  private void validatePageSpec(String statementId, PageSpec pageSpec) {
    int maxPageSize = properties.getMaxPageSize();

    if (maxPageSize < 1) {
      throw new PaginationException(
          "corwin.mybatis.pagination.max-page-size " + "must be greater than zero");
    }

    if (pageSpec.pageSize() > maxPageSize) {
      throw new PaginationException(
          "Page size "
              + pageSpec.pageSize()
              + " exceeds maximum "
              + maxPageSize
              + " for statement "
              + statementId);
    }
  }

  private long calculateOffset(PageSpec pageSpec) {
    return Math.multiplyExact((long) pageSpec.pageNo() - 1L, (long) pageSpec.pageSize());
  }

  private void validateResultMapping(MappedStatement statement) {
    boolean mapsDirectlyToPageData =
        statement.getResultMaps().stream()
            .anyMatch(resultMap -> PageData.class.equals(resultMap.getType()));

    if (mapsDirectlyToPageData) {
      throw new PaginationException(
          "XML resultType/resultMap of a paged statement "
              + "must describe the row element type, "
              + "not PageData: "
              + statement.getId());
    }
  }
}

package com.corwin.framework.mybatis.pagination;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.mybatis.pagination.count.JSqlParserCountSqlOptimizer;
import com.corwin.framework.mybatis.pagination.dialect.PageSql;
import com.corwin.framework.mybatis.pagination.dialect.PaginationDialect;
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

import java.util.List;
import java.util.Map;

/**
 * 将返回 PageData 且包含 PageSpec 参数的 MyBatis 查询自动改写为分页查询。
 *
 * @author Corwin 2026/7/28
 */
@Order(Ordered.LOWEST_PRECEDENCE)
@Intercepts({@Signature(type = Executor.class, method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class,
                        BoundSql.class})})
public final class PageDataPaginationInterceptor implements Interceptor {

    private static final String COUNT_SUFFIX = "!corwin-page-count";

    private static final String PAGE_SUFFIX = "!corwin-page-query";

    private final PageMethodResolver methodResolver;

    private final PaginationDialect dialect;

    private final JSqlParserCountSqlOptimizer countSqlOptimizer;

    private final MybatisPaginationProperties properties;

    public PageDataPaginationInterceptor(PageMethodResolver methodResolver, PaginationDialect dialect,
            JSqlParserCountSqlOptimizer countSqlOptimizer, MybatisPaginationProperties properties) {
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
         * 自定义 ResultHandler 不属于 PageData 自动分页场景。
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

        BoundSql originalBoundSql = args.length == 6 ? (BoundSql) args[5] : mappedStatement.getBoundSql(
                parameterObject);

        validateResultMapping(mappedStatement);

        Configuration configuration = mappedStatement.getConfiguration();

        Executor executor = (Executor) invocation.getTarget();

        String countSql = countSqlOptimizer.buildCountSql(originalBoundSql.getSql(),
                originalBoundSql.getParameterMappings().size());

        long total = executeCountQuery(executor, mappedStatement, parameterObject, originalBoundSql, countSql,
                configuration);

        /*
         * 提前结束，不再访问数据查询 SQL。
         */
        if (total == 0L || offset >= total) {
            PageData<Object> emptyPage = PageData.of(pageSpec, total, List.of());
            return List.of(emptyPage);
        }

        PageSql pageSql = dialect.buildPageSql(originalBoundSql.getSql(), offset, pageSpec.pageSize());

        BoundSql pageBoundSql = BoundSqlFactory.forPage(configuration, originalBoundSql, pageSql);

        MappedStatement pageMappedStatement = MappedStatementFactory.copy(mappedStatement,
                mappedStatement.getId() + PAGE_SUFFIX, new FixedBoundSqlSource(pageBoundSql),
                mappedStatement.getResultMaps());

        /*
         * 把当前查询调用替换为真正的分页数据查询。
         */
        args[0] = pageMappedStatement;
        args[2] = RowBounds.DEFAULT;

        if (args.length == 6) {
            CacheKey pageCacheKey = executor.createCacheKey(pageMappedStatement, parameterObject, RowBounds.DEFAULT,
                    pageBoundSql);

            args[4] = pageCacheKey;
            args[5] = pageBoundSql;
        }

        /*
         * MyBatis 按 XML 的 resultMap/resultType 映射为 List<Order>。
         */
        @SuppressWarnings("unchecked") List<Object> elements = (List<Object>) invocation.proceed();

        PageData<Object> pageData = PageData.of(pageSpec, total, elements);

        /*
         * MapperMethod 最终走 selectOne()。
         * selectOne() 要求底层结果列表只能有一个元素。
         */
        return List.of(pageData);
    }

    private long executeCountQuery(Executor executor, MappedStatement originalStatement, Object parameterObject,
            BoundSql originalBoundSql, String countSql, Configuration configuration) throws Exception {

        BoundSql countBoundSql = BoundSqlFactory.forCount(configuration, originalBoundSql, countSql);

        String countStatementId = originalStatement.getId() + COUNT_SUFFIX;

        ResultMap countResultMap = new ResultMap.Builder(configuration, countStatementId + "!result-map", Long.class,
                List.of()).build();

        MappedStatement countStatement = MappedStatementFactory.copy(originalStatement, countStatementId,
                new FixedBoundSqlSource(countBoundSql), List.of(countResultMap));

        CacheKey countCacheKey = executor.createCacheKey(countStatement, parameterObject, RowBounds.DEFAULT,
                countBoundSql);

        /*
         * invocation.getTarget() 是当前插件代理内部的 Executor。
         * 直接调用它不会再次进入当前分页拦截器，
         * 因而不需要 ThreadLocal 防递归。
         */
        List<?> countResult = executor.query(countStatement, parameterObject, RowBounds.DEFAULT,
                Executor.NO_RESULT_HANDLER, countCacheKey, countBoundSql);

        if (countResult == null || countResult.isEmpty() || countResult.getFirst() == null) {
            return 0L;
        }

        if (countResult.size() != 1) {
            throw new PaginationException(
                    "Count query returned " + countResult.size() + " rows for statement " + originalStatement.getId());
        }

        Object value = countResult.getFirst();

        if (value instanceof Number number) {
            return number.longValue();
        }

        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException exception) {
            throw new PaginationException(
                    "Count query did not return a numeric value " + "for statement " + originalStatement.getId() + ": " + value,
                    exception);
        }
    }

    private PageSpec resolvePageSpec(Object parameterObject) {
        if (parameterObject instanceof PageSpec pageSpec) {
            return pageSpec;
        }

        /*
         * 多参数 Mapper 方法会被 MyBatis 包装成 ParamMap。
         * 相同参数可能同时以 page 和 param1 两个 key 存在，
         * 找到第一个 PageSpec 即可。
         */
        if (parameterObject instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                if (value instanceof PageSpec pageSpec) {
                    return pageSpec;
                }
            }
        }

        /*
         * 方法签名已经确认存在 PageSpec，
         * 运行时没有找到对象时视为传入 null，使用默认分页值。
         */
        return PageSpec.of(null, null, null);
    }

    private void validatePageSpec(String statementId, PageSpec pageSpec) {
        int maxPageSize = properties.getMaxPageSize();

        if (maxPageSize < 1) {
            throw new PaginationException("corwin.mybatis.pagination.max-page-size " + "must be greater than zero");
        }

        if (pageSpec.pageSize() > maxPageSize) {
            throw new PaginationException(
                    "Page size " + pageSpec.pageSize() + " exceeds maximum " + maxPageSize + " for statement " + statementId);
        }
    }

    private long calculateOffset(PageSpec pageSpec) {
        return Math.multiplyExact((long) pageSpec.pageNo() - 1L, (long) pageSpec.pageSize());
    }

    private void validateResultMapping(MappedStatement statement) {
        boolean mapsDirectlyToPageData = statement.getResultMaps().stream()
                .anyMatch(resultMap -> PageData.class.equals(resultMap.getType()));

        if (mapsDirectlyToPageData) {
            throw new PaginationException(
                    "XML resultType/resultMap of a paged statement " + "must describe the row element type, " + "not PageData: " + statement.getId());
        }
    }
}

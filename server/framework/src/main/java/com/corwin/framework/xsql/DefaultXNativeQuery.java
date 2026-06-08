package com.corwin.framework.xsql;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.xsql.dialect.XSqlDialect;
import com.corwin.framework.xsql.error.XSqlExecuteException;
import com.corwin.framework.xsql.error.XSqlQueryBuildException;
import com.corwin.framework.xsql.error.XSqlResultMappingException;
import com.corwin.framework.xsql.error.XSqlTooManyResultsException;
import com.corwin.framework.xsql.support.XSqlAliasParser;
import com.corwin.framework.xsql.support.XSqlBuilderResult;
import com.corwin.framework.xsql.support.XSqlOperator;
import com.corwin.framework.xsql.support.XSqlOrder;
import com.corwin.framework.xsql.support.XSqlOrderBuilder;
import com.corwin.framework.xsql.support.XSqlPredicate;
import com.corwin.framework.xsql.support.XSqlResultMapper;
import com.corwin.framework.xsql.support.XSqlWhereBuilder;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * XSql 原生 SQL 查询实现。
 * <p>
 * 设计思路：
 * <ul>
 *     <li>由调用方提供基础 SELECT SQL 模板。</li>
 *     <li>运行时仅追加动态 where/order/page 片段。</li>
 *     <li>使用 alias 白名单控制排序与映射安全。</li>
 * </ul>
 * 该实现不推导关系结构，聚焦“受控动态拼装 + 结果映射”。
 *
 * @author Corwin 2026/4/9
 */
final class DefaultXNativeQuery<R> implements XNativeQuery<R> {

    private final DataSource dataSource;
    private final XSqlDialect dialect;
    private final Class<R> resultClass;
    private final Map<String, String> aliasToProperty;
    private final List<XSqlPredicate> predicates;
    private final List<XSqlOrder> orders;
    private String sqlTemplate;
    private String countSqlTemplate;
    private Set<String> selectAliases;

    DefaultXNativeQuery(DataSource dataSource, XSqlDialect dialect, Class<R> resultClass) {
        this.dataSource = dataSource;
        this.dialect = dialect;
        this.resultClass = resultClass;
        this.aliasToProperty = XSqlResultMapper.buildNativeAliasMap(resultClass);
        this.predicates = new ArrayList<>();
        this.orders = new ArrayList<>();
        this.selectAliases = Set.of();
    }

    /**
     * 设置原生 SQL 模板并解析 alias 白名单。
     */
    @Override
    public XNativeQuery<R> sql(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new XSqlQueryBuildException("Native SQL template must not be blank");
        }
        Set<String> parsedAliases = XSqlAliasParser.parseSelectAliases(sql);
        validateAliasMapping(parsedAliases);
        this.sqlTemplate = sql.trim();
        this.selectAliases = parsedAliases;
        return this;
    }

    /**
     * 设置手工 count SQL 模板。
     */
    @Override
    public XNativeQuery<R> countSql(String countSql) {
        if (countSql == null || countSql.isBlank()) {
            throw new XSqlQueryBuildException("countSql must not be blank");
        }
        this.countSqlTemplate = countSql.trim();
        return this;
    }

    /**
     * 添加等值条件。
     */
    @Override
    public XNativeQuery<R> eq(String columnExpr, Object value) {
        predicates.add(new XSqlPredicate(requireExpr(columnExpr), XSqlOperator.EQ, List.of(value)));
        return this;
    }

    @Override
    public XNativeQuery<R> eqIf(boolean condition, String columnExpr, Object value) {
        if (!condition) {
            return this;
        }
        return eq(columnExpr, value);
    }

    @Override
    public XNativeQuery<R> likeIf(boolean condition, String columnExpr, String value) {
        if (!condition) {
            return this;
        }
        predicates.add(new XSqlPredicate(requireExpr(columnExpr), XSqlOperator.LIKE, List.of(value)));
        return this;
    }

    @Override
    public XNativeQuery<R> inIf(boolean condition, String columnExpr, Collection<?> values) {
        if (!condition) {
            return this;
        }
        if (values == null || values.isEmpty()) {
            throw new XSqlQueryBuildException("IN values must not be empty, expr=" + columnExpr);
        }
        predicates.add(new XSqlPredicate(requireExpr(columnExpr), XSqlOperator.IN, List.of(values)));
        return this;
    }

    @Override
    public XNativeQuery<R> betweenIf(boolean condition, String columnExpr, Object start, Object end) {
        if (!condition) {
            return this;
        }
        predicates.add(new XSqlPredicate(requireExpr(columnExpr), XSqlOperator.BETWEEN, List.of(start, end)));
        return this;
    }

    @Override
    public XNativeQuery<R> geIf(boolean condition, String columnExpr, Object value) {
        if (!condition) {
            return this;
        }
        predicates.add(new XSqlPredicate(requireExpr(columnExpr), XSqlOperator.GE, List.of(value)));
        return this;
    }

    @Override
    public XNativeQuery<R> leIf(boolean condition, String columnExpr, Object value) {
        if (!condition) {
            return this;
        }
        predicates.add(new XSqlPredicate(requireExpr(columnExpr), XSqlOperator.LE, List.of(value)));
        return this;
    }

    @Override
    public XNativeQuery<R> isNullIf(boolean condition, String columnExpr) {
        if (!condition) {
            return this;
        }
        predicates.add(new XSqlPredicate(requireExpr(columnExpr), XSqlOperator.IS_NULL, List.of()));
        return this;
    }

    @Override
    public XNativeQuery<R> isNotNullIf(boolean condition, String columnExpr) {
        if (!condition) {
            return this;
        }
        predicates.add(new XSqlPredicate(requireExpr(columnExpr), XSqlOperator.IS_NOT_NULL, List.of()));
        return this;
    }

    /**
     * 按 alias 排序，alias 必须在 SELECT 白名单中。
     */
    @Override
    public XNativeQuery<R> orderByAlias(String alias, XSortDirection direction) {
        ensureSqlReady();
        String normalized = normalize(alias);
        if (!selectAliases.contains(normalized)) {
            throw new XSqlQueryBuildException("Sort alias is not in select alias whitelist: " + alias);
        }
        XSortDirection safeDirection = direction == null ? XSortDirection.ASC : direction;
        orders.add(new XSqlOrder(dialect.quote(alias.trim()), safeDirection));
        return this;
    }

    @Override
    public XNativeQuery<R> orderByAliasIf(boolean condition, String alias, XSortDirection direction) {
        if (!condition) {
            return this;
        }
        return orderByAlias(alias, direction);
    }

    /**
     * 执行列表查询。
     */
    @Override
    public List<R> list() {
        ensureSqlReady();
        XSqlBuilderResult where = buildWhereSql(sqlTemplate);
        String sql = XSqlOrderBuilder.append(where.sql(), orders);
        return executeList(sql, where.params());
    }

    /**
     * 执行单条查询；超过一条视为数据语义错误。
     */
    @Override
    public R one() {
        List<R> rows = list();
        if (rows.isEmpty()) {
            return null;
        }
        if (rows.size() > 1) {
            throw new XSqlTooManyResultsException(
                    "XSql native one() expected <= 1 row, actual=" + rows.size() + ", resultClass=" +
                            resultClass.getName());
        }
        return rows.getFirst();
    }

    /**
     * 执行分页查询，自动处理 count 逻辑。
     */
    @Override
    public PageData<R> page(int pageNo, int pageSize) {
        validatePageArg(pageNo, pageSize);
        ensureSqlReady();

        XSqlBuilderResult where = buildWhereSql(sqlTemplate);
        String orderSql = XSqlOrderBuilder.append(where.sql(), orders);
        String pageSql = dialect.buildPageSql(orderSql);

        List<Object> pageParams = new ArrayList<>(where.params());
        int offset = (pageNo - 1) * pageSize;
        pageParams.add(offset);
        pageParams.add(pageSize);
        List<R> rows = executeList(pageSql, pageParams);
        long total = executeCount(where);

        return PageData.of(pageNo, pageSize, total, rows);
    }

    /**
     * 执行统计查询：优先手工 countSql，否则使用自动包装 count。
     */
    private long executeCount(XSqlBuilderResult whereResultForBaseSql) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        try {
            if (countSqlTemplate != null && !countSqlTemplate.isBlank()) {
                XSqlBuilderResult countWhere = buildWhereSql(countSqlTemplate);
                Long count = jdbcTemplate.queryForObject(countWhere.sql(), Long.class, countWhere.params().toArray());
                return count == null ? 0L : count;
            }
            String wrapped = dialect.buildCountWrapperSql(whereResultForBaseSql.sql());
            Long count = jdbcTemplate.queryForObject(wrapped, Long.class, whereResultForBaseSql.params().toArray());
            return count == null ? 0L : count;
        } catch (Exception ex) {
            throw new XSqlExecuteException("Execute XSql native count failed", ex);
        }
    }

    /**
     * 执行查询并映射结果。
     */
    private List<R> executeList(String sql, List<Object> params) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        try {
            return jdbcTemplate.query(sql, rs -> {
                List<R> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(XSqlResultMapper.mapNativeRow(rs, resultClass, aliasToProperty));
                }
                return rows;
            }, params.toArray());
        } catch (Exception ex) {
            throw new XSqlExecuteException("Execute XSql native list failed, sql=" + sql, ex);
        }
    }

    /**
     * 构建 where SQL，自动适配基础 SQL 是否已包含 where 关键字。
     */
    private XSqlBuilderResult buildWhereSql(String baseSql) {
        boolean hasWhere = XSqlAliasParser.containsWhereKeyword(" " + baseSql.toLowerCase(Locale.ROOT) + " ");
        return XSqlWhereBuilder.build(baseSql, predicates, hasWhere);
    }

    /**
     * 校验 SQL 模板是否已设置。
     */
    private void ensureSqlReady() {
        if (sqlTemplate == null || sqlTemplate.isBlank()) {
            throw new XSqlQueryBuildException("Native SQL template is not set, call sql(...) first");
        }
    }

    /**
     * 校验并归一列表达式。
     */
    private String requireExpr(String expr) {
        Objects.requireNonNull(expr, "columnExpr must not be null");
        String trimmed = expr.trim();
        if (trimmed.isEmpty()) {
            throw new XSqlQueryBuildException("columnExpr must not be blank");
        }
        return trimmed;
    }

    /**
     * 校验 SQL 中出现的 alias 在结果映射中都可落地。
     */
    private void validateAliasMapping(Set<String> aliases) {
        Set<String> missing = new LinkedHashSet<>();
        for (String alias : aliases) {
            if (!aliasToProperty.containsKey(alias)) {
                missing.add(alias);
            }
        }
        if (!missing.isEmpty()) {
            throw new XSqlResultMappingException(
                "Native SQL aliases not mapped in result class " + resultClass.getName() + ": " + missing);
        }
    }

    /**
     * alias 归一化（trim + 小写）。
     */
    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 校验分页参数。
     */
    private void validatePageArg(int pageNo, int pageSize) {
        if (pageNo < 1 || pageSize < 1) {
            throw new XSqlQueryBuildException("Invalid page argument, pageNo=" + pageNo + ", pageSize=" + pageSize);
        }
    }
}


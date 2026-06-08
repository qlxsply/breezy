package com.corwin.framework.xsql;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.xsql.codec.XValueCodec;
import com.corwin.framework.xsql.dialect.XSqlDialect;
import com.corwin.framework.xsql.error.XSqlExecuteException;
import com.corwin.framework.xsql.error.XSqlQueryBuildException;
import com.corwin.framework.xsql.error.XSqlTooManyResultsException;
import com.corwin.framework.xsql.meta.XColumnMeta;
import com.corwin.framework.xsql.meta.XEntityMeta;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * XSql 单表查询实现。
 * <p>
 * 基于预解析实体元数据执行动态查询，核心原则：
 * <ul>
 *     <li>条件字段、排序字段必须命中元数据白名单。</li>
 *     <li>参数统一通过预编译绑定。</li>
 *     <li>值转换统一通过列级 {@link XValueCodec} 处理。</li>
 * </ul>
 *
 * @author Corwin 2026/4/9
 */
final class DefaultXTableQuery<E, R> implements XTableQuery<E, R> {

    private final DataSource dataSource;
    private final XSqlDialect dialect;
    private final XEntityMeta meta;
    private final Class<R> resultClass;
    private final List<XSqlPredicate> predicates;
    private final List<XSqlOrder> orders;

    DefaultXTableQuery(DataSource dataSource, XSqlDialect dialect, XEntityMeta meta, Class<R> resultClass) {
        this.dataSource = dataSource;
        this.dialect = dialect;
        this.meta = meta;
        this.resultClass = resultClass;
        this.predicates = new ArrayList<>();
        this.orders = new ArrayList<>();
    }

    @Override
    public XTableQuery<E, R> eq(String propertyPath, Object value) {
        return addSingleValuePredicate(propertyPath, XSqlOperator.EQ, value);
    }

    @Override
    public XTableQuery<E, R> eq(XGetter<E, ?> getter, Object value) {
        return eq(resolvePropertyPath(getter), value);
    }

    @Override
    public XTableQuery<E, R> eqIf(boolean condition, String propertyPath, Object value) {
        return condition ? eq(propertyPath, value) : this;
    }

    @Override
    public XTableQuery<E, R> eqIf(boolean condition, XGetter<E, ?> getter, Object value) {
        return condition ? eq(getter, value) : this;
    }

    @Override
    public XTableQuery<E, R> ne(String propertyPath, Object value) {
        return addSingleValuePredicate(propertyPath, XSqlOperator.NE, value);
    }

    @Override
    public XTableQuery<E, R> ne(XGetter<E, ?> getter, Object value) {
        return ne(resolvePropertyPath(getter), value);
    }

    @Override
    public XTableQuery<E, R> neIf(boolean condition, String propertyPath, Object value) {
        return condition ? ne(propertyPath, value) : this;
    }

    @Override
    public XTableQuery<E, R> neIf(boolean condition, XGetter<E, ?> getter, Object value) {
        return condition ? ne(getter, value) : this;
    }

    @Override
    public XTableQuery<E, R> gt(String propertyPath, Object value) {
        return addSingleValuePredicate(propertyPath, XSqlOperator.GT, value);
    }

    @Override
    public XTableQuery<E, R> gt(XGetter<E, ?> getter, Object value) {
        return gt(resolvePropertyPath(getter), value);
    }

    @Override
    public XTableQuery<E, R> gtIf(boolean condition, String propertyPath, Object value) {
        return condition ? gt(propertyPath, value) : this;
    }

    @Override
    public XTableQuery<E, R> gtIf(boolean condition, XGetter<E, ?> getter, Object value) {
        return condition ? gt(getter, value) : this;
    }

    @Override
    public XTableQuery<E, R> lt(String propertyPath, Object value) {
        return addSingleValuePredicate(propertyPath, XSqlOperator.LT, value);
    }

    @Override
    public XTableQuery<E, R> lt(XGetter<E, ?> getter, Object value) {
        return lt(resolvePropertyPath(getter), value);
    }

    @Override
    public XTableQuery<E, R> ltIf(boolean condition, String propertyPath, Object value) {
        return condition ? lt(propertyPath, value) : this;
    }

    @Override
    public XTableQuery<E, R> ltIf(boolean condition, XGetter<E, ?> getter, Object value) {
        return condition ? lt(getter, value) : this;
    }

    @Override
    public XTableQuery<E, R> ge(String propertyPath, Object value) {
        return addSingleValuePredicate(propertyPath, XSqlOperator.GE, value);
    }

    @Override
    public XTableQuery<E, R> ge(XGetter<E, ?> getter, Object value) {
        return ge(resolvePropertyPath(getter), value);
    }

    @Override
    public XTableQuery<E, R> geIf(boolean condition, String propertyPath, Object value) {
        return condition ? ge(propertyPath, value) : this;
    }

    @Override
    public XTableQuery<E, R> geIf(boolean condition, XGetter<E, ?> getter, Object value) {
        return condition ? ge(getter, value) : this;
    }

    @Override
    public XTableQuery<E, R> le(String propertyPath, Object value) {
        return addSingleValuePredicate(propertyPath, XSqlOperator.LE, value);
    }

    @Override
    public XTableQuery<E, R> le(XGetter<E, ?> getter, Object value) {
        return le(resolvePropertyPath(getter), value);
    }

    @Override
    public XTableQuery<E, R> leIf(boolean condition, String propertyPath, Object value) {
        return condition ? le(propertyPath, value) : this;
    }

    @Override
    public XTableQuery<E, R> leIf(boolean condition, XGetter<E, ?> getter, Object value) {
        return condition ? le(getter, value) : this;
    }

    @Override
    public XTableQuery<E, R> like(String propertyPath, String value) {
        return addSingleValuePredicate(propertyPath, XSqlOperator.LIKE, value);
    }

    @Override
    public XTableQuery<E, R> like(XGetter<E, ?> getter, String value) {
        return like(resolvePropertyPath(getter), value);
    }

    @Override
    public XTableQuery<E, R> likeIf(boolean condition, String propertyPath, String value) {
        return condition ? like(propertyPath, value) : this;
    }

    @Override
    public XTableQuery<E, R> likeIf(boolean condition, XGetter<E, ?> getter, String value) {
        return condition ? like(getter, value) : this;
    }

    @Override
    public XTableQuery<E, R> leftLike(String propertyPath, String value) {
        return addSingleValuePredicate(propertyPath, XSqlOperator.LEFT_LIKE, value);
    }

    @Override
    public XTableQuery<E, R> leftLike(XGetter<E, ?> getter, String value) {
        return leftLike(resolvePropertyPath(getter), value);
    }

    @Override
    public XTableQuery<E, R> leftLikeIf(boolean condition, String propertyPath, String value) {
        return condition ? leftLike(propertyPath, value) : this;
    }

    @Override
    public XTableQuery<E, R> leftLikeIf(boolean condition, XGetter<E, ?> getter, String value) {
        return condition ? leftLike(getter, value) : this;
    }

    @Override
    public XTableQuery<E, R> rightLike(String propertyPath, String value) {
        return addSingleValuePredicate(propertyPath, XSqlOperator.RIGHT_LIKE, value);
    }

    @Override
    public XTableQuery<E, R> rightLike(XGetter<E, ?> getter, String value) {
        return rightLike(resolvePropertyPath(getter), value);
    }

    @Override
    public XTableQuery<E, R> rightLikeIf(boolean condition, String propertyPath, String value) {
        return condition ? rightLike(propertyPath, value) : this;
    }

    @Override
    public XTableQuery<E, R> rightLikeIf(boolean condition, XGetter<E, ?> getter, String value) {
        return condition ? rightLike(getter, value) : this;
    }

    @Override
    public XTableQuery<E, R> in(String propertyPath, Collection<?> values) {
        return addCollectionPredicate(propertyPath, XSqlOperator.IN, values);
    }

    @Override
    public XTableQuery<E, R> in(XGetter<E, ?> getter, Collection<?> values) {
        return in(resolvePropertyPath(getter), values);
    }

    @Override
    public XTableQuery<E, R> inIf(boolean condition, String propertyPath, Collection<?> values) {
        return condition ? in(propertyPath, values) : this;
    }

    @Override
    public XTableQuery<E, R> inIf(boolean condition, XGetter<E, ?> getter, Collection<?> values) {
        return condition ? in(getter, values) : this;
    }

    @Override
    public XTableQuery<E, R> notIn(String propertyPath, Collection<?> values) {
        return addCollectionPredicate(propertyPath, XSqlOperator.NOT_IN, values);
    }

    @Override
    public XTableQuery<E, R> notIn(XGetter<E, ?> getter, Collection<?> values) {
        return notIn(resolvePropertyPath(getter), values);
    }

    @Override
    public XTableQuery<E, R> notInIf(boolean condition, String propertyPath, Collection<?> values) {
        return condition ? notIn(propertyPath, values) : this;
    }

    @Override
    public XTableQuery<E, R> notInIf(boolean condition, XGetter<E, ?> getter, Collection<?> values) {
        return condition ? notIn(getter, values) : this;
    }

    @Override
    public XTableQuery<E, R> between(String propertyPath, Object start, Object end) {
        return addRangePredicate(propertyPath, XSqlOperator.BETWEEN, start, end);
    }

    @Override
    public XTableQuery<E, R> between(XGetter<E, ?> getter, Object start, Object end) {
        return between(resolvePropertyPath(getter), start, end);
    }

    @Override
    public XTableQuery<E, R> betweenIf(boolean condition, String propertyPath, Object start, Object end) {
        return condition ? between(propertyPath, start, end) : this;
    }

    @Override
    public XTableQuery<E, R> betweenIf(boolean condition, XGetter<E, ?> getter, Object start, Object end) {
        return condition ? between(getter, start, end) : this;
    }

    @Override
    public XTableQuery<E, R> notBetween(String propertyPath, Object start, Object end) {
        return addRangePredicate(propertyPath, XSqlOperator.NOT_BETWEEN, start, end);
    }

    @Override
    public XTableQuery<E, R> notBetween(XGetter<E, ?> getter, Object start, Object end) {
        return notBetween(resolvePropertyPath(getter), start, end);
    }

    @Override
    public XTableQuery<E, R> notBetweenIf(boolean condition, String propertyPath, Object start, Object end) {
        return condition ? notBetween(propertyPath, start, end) : this;
    }

    @Override
    public XTableQuery<E, R> notBetweenIf(boolean condition, XGetter<E, ?> getter, Object start, Object end) {
        return condition ? notBetween(getter, start, end) : this;
    }

    @Override
    public XTableQuery<E, R> isNull(String propertyPath) {
        return addNoValuePredicate(propertyPath, XSqlOperator.IS_NULL);
    }

    @Override
    public XTableQuery<E, R> isNull(XGetter<E, ?> getter) {
        return isNull(resolvePropertyPath(getter));
    }

    @Override
    public XTableQuery<E, R> isNullIf(boolean condition, String propertyPath) {
        return condition ? isNull(propertyPath) : this;
    }

    @Override
    public XTableQuery<E, R> isNullIf(boolean condition, XGetter<E, ?> getter) {
        return condition ? isNull(getter) : this;
    }

    @Override
    public XTableQuery<E, R> isNotNull(String propertyPath) {
        return addNoValuePredicate(propertyPath, XSqlOperator.IS_NOT_NULL);
    }

    @Override
    public XTableQuery<E, R> isNotNull(XGetter<E, ?> getter) {
        return isNotNull(resolvePropertyPath(getter));
    }

    @Override
    public XTableQuery<E, R> isNotNullIf(boolean condition, String propertyPath) {
        return condition ? isNotNull(propertyPath) : this;
    }

    @Override
    public XTableQuery<E, R> isNotNullIf(boolean condition, XGetter<E, ?> getter) {
        return condition ? isNotNull(getter) : this;
    }

    @Override
    public XTableQuery<E, R> orderBy(String propertyPath, XSortDirection direction) {
        XColumnMeta column = requireSortableColumn(propertyPath);
        XSortDirection safeDirection = direction == null ? XSortDirection.ASC : direction;
        orders.add(new XSqlOrder(toColumnExpr(column), safeDirection));
        return this;
    }

    @Override
    public XTableQuery<E, R> orderBy(XGetter<E, ?> getter, XSortDirection direction) {
        return orderBy(resolvePropertyPath(getter), direction);
    }

    @Override
    public XTableQuery<E, R> orderByIf(boolean condition, String propertyPath, XSortDirection direction) {
        return condition ? orderBy(propertyPath, direction) : this;
    }

    @Override
    public XTableQuery<E, R> orderByIf(boolean condition, XGetter<E, ?> getter, XSortDirection direction) {
        return condition ? orderBy(getter, direction) : this;
    }

    /**
     * 执行列表查询。
     */
    @Override
    public List<R> list() {
        XSqlBuilderResult whereResult = XSqlWhereBuilder.build(meta.baseSelectSql(), predicates, false);
        String sql = XSqlOrderBuilder.append(whereResult.sql(), orders);
        return executeList(sql, whereResult.params());
    }

    /**
     * 执行单条查询；返回多条时抛异常。
     */
    @Override
    public R one() {
        List<R> rows = list();
        if (rows.isEmpty()) {
            return null;
        }
        if (rows.size() > 1) {
            throw new XSqlTooManyResultsException(
                    "XSql one() expected <= 1 row, actual=" + rows.size() + ", entity=" + meta.entityClass().getName());
        }
        return rows.getFirst();
    }

    /**
     * 执行分页查询。
     */
    @Override
    public PageData<R> page(int pageNo, int pageSize) {
        validatePageArg(pageNo, pageSize);
        XSqlBuilderResult whereResult = XSqlWhereBuilder.build(meta.baseSelectSql(), predicates, false);
        String orderSql = XSqlOrderBuilder.append(whereResult.sql(), orders);
        String pageSql = dialect.buildPageSql(orderSql);

        List<Object> pageParams = new ArrayList<>(whereResult.params());
        int offset = (pageNo - 1) * pageSize;
        pageParams.add(offset);
        pageParams.add(pageSize);

        List<R> rows = executeList(pageSql, pageParams);
        long total = executeCount();
        return PageData.of(pageNo, pageSize, total, rows);
    }

    /**
     * 添加单值谓词。
     */
    private XTableQuery<E, R> addSingleValuePredicate(String propertyPath, XSqlOperator operator, Object value) {
        XColumnMeta column = requireConditionableColumn(propertyPath);
        predicates.add(new XSqlPredicate(toColumnExpr(column), operator, List.of(encode(column, value))));
        return this;
    }

    /**
     * 添加集合谓词（in/not in）。
     */
    private XTableQuery<E, R> addCollectionPredicate(String propertyPath, XSqlOperator operator, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            throw new XSqlQueryBuildException(operator + " values must not be empty, property=" + propertyPath);
        }
        XColumnMeta column = requireConditionableColumn(propertyPath);
        List<Object> encoded = new ArrayList<>(values.size());
        for (Object value : values) {
            encoded.add(encode(column, value));
        }
        predicates.add(new XSqlPredicate(toColumnExpr(column), operator, List.of(encoded)));
        return this;
    }

    /**
     * 添加区间谓词（between/not between）。
     */
    private XTableQuery<E, R> addRangePredicate(String propertyPath, XSqlOperator operator, Object start, Object end) {
        XColumnMeta column = requireConditionableColumn(propertyPath);
        predicates.add(new XSqlPredicate(toColumnExpr(column), operator,
                List.of(encode(column, start), encode(column, end))));
        return this;
    }

    /**
     * 添加无参谓词（is null / is not null）。
     */
    private XTableQuery<E, R> addNoValuePredicate(String propertyPath, XSqlOperator operator) {
        XColumnMeta column = requireConditionableColumn(propertyPath);
        predicates.add(new XSqlPredicate(toColumnExpr(column), operator, List.of()));
        return this;
    }

    /**
     * 解析 Lambda Getter 对应属性路径。
     */
    private String resolvePropertyPath(XGetter<E, ?> getter) {
        return XLambdaPropertyResolver.resolve(getter);
    }

    /**
     * 执行统计查询。
     */
    private long executeCount() {
        XSqlBuilderResult whereResult = XSqlWhereBuilder.build(meta.baseCountSql(), predicates, false);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        try {
            Long count = jdbcTemplate.queryForObject(whereResult.sql(), Long.class, whereResult.params().toArray());
            return count == null ? 0L : count;
        } catch (Exception ex) {
            throw new XSqlExecuteException("Execute XSql count failed, sql=" + whereResult.sql(), ex);
        }
    }

    /**
     * 执行 SQL 并映射列表结果。
     */
    private List<R> executeList(String sql, List<Object> params) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        try {
            return jdbcTemplate.query(sql, rs -> {
                List<R> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(XSqlResultMapper.mapTableRow(rs, meta, resultClass));
                }
                return result;
            }, params.toArray());
        } catch (Exception ex) {
            throw new XSqlExecuteException("Execute XSql list failed, sql=" + sql, ex);
        }
    }

    /**
     * 读取可条件列。
     */
    private XColumnMeta requireConditionableColumn(String propertyPath) {
        return requireColumn(meta.conditionableColumns(), propertyPath, "condition");
    }

    /**
     * 读取可排序列。
     */
    private XColumnMeta requireSortableColumn(String propertyPath) {
        return requireColumn(meta.sortableColumns(), propertyPath, "sort");
    }

    /**
     * 校验属性路径存在于指定列映射中。
     */
    private XColumnMeta requireColumn(Map<String, XColumnMeta> source, String propertyPath, String usage) {
        Objects.requireNonNull(propertyPath, "propertyPath must not be null");
        XColumnMeta column = source.get(propertyPath);
        if (column == null) {
            throw new XSqlQueryBuildException("Property path is not available for " + usage + ": " + propertyPath);
        }
        return column;
    }

    /**
     * 构建方言安全列表达式（t.`column`）。
     */
    private String toColumnExpr(XColumnMeta columnMeta) {
        return meta.tableAlias() + "." + dialect.quote(columnMeta.columnName());
    }

    /**
     * 使用列级 codec 编码查询参数。
     */
    private Object encode(XColumnMeta columnMeta, Object value) {
        XValueCodec codec = columnMeta.valueCodec();
        return codec == null ? value : codec.toDbValue(value);
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

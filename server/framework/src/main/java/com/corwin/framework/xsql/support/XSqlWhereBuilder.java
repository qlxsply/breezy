package com.corwin.framework.xsql.support;

import com.corwin.framework.xsql.error.XSqlQueryBuildException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * XSql Where 子句构建器。
 * <p>
 * 负责将谓词列表转换为参数化 WHERE 片段，并维护参数顺序。
 *
 * @author Corwin 2026/4/9
 */
public final class XSqlWhereBuilder {

    private XSqlWhereBuilder() {
    }

    /**
     * 构建 where 子句。
     *
     * @param baseSql 基础 SQL
     * @param predicates 谓词列表
     * @param hasWhereKeyword 基础 SQL 是否已包含 where
     */
    public static XSqlBuilderResult build(String baseSql, List<XSqlPredicate> predicates, boolean hasWhereKeyword) {
        if (predicates == null || predicates.isEmpty()) {
            return new XSqlBuilderResult(baseSql, List.of());
        }
        StringBuilder sql = new StringBuilder(baseSql);
        List<Object> params = new ArrayList<>();

        sql.append(hasWhereKeyword ? " and " : " where ");
        int index = 0;
        for (XSqlPredicate predicate : predicates) {
            if (index++ > 0) {
                sql.append(" and ");
            }
            appendPredicate(sql, params, predicate);
        }
        return new XSqlBuilderResult(sql.toString(), params);
    }

    /**
     * 追加单个谓词。
     */
    private static void appendPredicate(StringBuilder sql, List<Object> params, XSqlPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        String field = predicate.fieldExpr();
        if (field == null || field.isBlank()) {
            throw new XSqlQueryBuildException("Field expression must not be blank");
        }
        XSqlOperator operator = Objects.requireNonNull(predicate.operator(), "operator must not be null");
        List<Object> values = predicate.values() == null ? List.of() : predicate.values();

        switch (operator) {
            case EQ -> appendSingleValue(sql, params, field, " = ?", operator, values);
            case NE -> appendSingleValue(sql, params, field, " <> ?", operator, values);
            case GT -> appendSingleValue(sql, params, field, " > ?", operator, values);
            case LT -> appendSingleValue(sql, params, field, " < ?", operator, values);
            case GE -> appendSingleValue(sql, params, field, " >= ?", operator, values);
            case LE -> appendSingleValue(sql, params, field, " <= ?", operator, values);
            case LIKE -> appendSingleValue(sql, params, field, " like ?", operator, values);
            case LEFT_LIKE -> appendLikeValue(sql, params, field, operator, values, true, false);
            case RIGHT_LIKE -> appendLikeValue(sql, params, field, operator, values, false, true);
            case IN -> appendCollectionValue(sql, params, field, "in", operator, values);
            case NOT_IN -> appendCollectionValue(sql, params, field, "not in", operator, values);
            case BETWEEN -> appendRangeValue(sql, params, field, "between", operator, values);
            case NOT_BETWEEN -> appendRangeValue(sql, params, field, "not between", operator, values);
            case IS_NULL -> {
                ensureNoValue(operator, values);
                sql.append(field).append(" is null");
            }
            case IS_NOT_NULL -> {
                ensureNoValue(operator, values);
                sql.append(field).append(" is not null");
            }
            default -> throw new XSqlQueryBuildException("Unsupported operator: " + operator);
        }
    }

    /**
     * 追加单值条件。
     */
    private static void appendSingleValue(StringBuilder sql, List<Object> params, String field, String expression,
            XSqlOperator operator, List<Object> values) {
        sql.append(field).append(expression);
        params.add(requireSingleValue(operator, values));
    }

    /**
     * 追加 like 条件（支持左右通配）。
     */
    private static void appendLikeValue(StringBuilder sql, List<Object> params, String field, XSqlOperator operator,
            List<Object> values, boolean prefixWildcard, boolean suffixWildcard) {
        Object raw = requireSingleValue(operator, values);
        if (raw == null) {
            throw new XSqlQueryBuildException(operator + " condition requires non-null value");
        }
        String likeValue = raw.toString();
        if (prefixWildcard) {
            likeValue = "%" + likeValue;
        }
        if (suffixWildcard) {
            likeValue = likeValue + "%";
        }
        sql.append(field).append(" like ?");
        params.add(likeValue);
    }

    /**
     * 追加 in/not in 条件。
     */
    private static void appendCollectionValue(StringBuilder sql, List<Object> params, String field, String keyword,
            XSqlOperator operator, List<Object> values) {
        Collection<?> collection = resolveCollectionValues(operator, values);
        sql.append(field).append(' ').append(keyword).append(" (");
        int index = 0;
        for (Object item : collection) {
            if (index++ > 0) {
                sql.append(", ");
            }
            sql.append('?');
            params.add(item);
        }
        sql.append(')');
    }

    /**
     * 追加 between/not between 条件。
     */
    private static void appendRangeValue(StringBuilder sql, List<Object> params, String field, String keyword,
            XSqlOperator operator, List<Object> values) {
        if (values.size() != 2) {
            throw new XSqlQueryBuildException(operator + " condition requires exactly two values");
        }
        sql.append(field).append(' ').append(keyword).append(" ? and ?");
        params.add(values.get(0));
        params.add(values.get(1));
    }

    /**
     * 校验并提取单值。
     */
    private static Object requireSingleValue(XSqlOperator operator, List<Object> values) {
        if (values.size() != 1) {
            throw new XSqlQueryBuildException(operator + " condition requires exactly one value");
        }
        return values.getFirst();
    }

    /**
     * 校验并提取集合值。
     */
    private static Collection<?> resolveCollectionValues(XSqlOperator operator, List<Object> values) {
        if (values.isEmpty()) {
            throw new XSqlQueryBuildException(operator + " condition requires non-empty collection");
        }
        if (values.size() == 1 && values.getFirst() instanceof Collection<?> collection) {
            if (collection.isEmpty()) {
                throw new XSqlQueryBuildException(operator + " condition requires non-empty collection");
            }
            return collection;
        }
        return values;
    }

    /**
     * 校验无值操作符不携带参数。
     */
    private static void ensureNoValue(XSqlOperator operator, List<Object> values) {
        if (!values.isEmpty()) {
            throw new XSqlQueryBuildException(operator + " condition does not accept values");
        }
    }
}

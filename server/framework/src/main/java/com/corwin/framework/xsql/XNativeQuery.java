package com.corwin.framework.xsql;

import com.corwin.framework.domain.page.PageData;

import java.util.Collection;
import java.util.List;

/**
 * 原生 SQL 查询 DSL。
 * <p>
 * 用于复杂查询场景。调用约定：
 * 先设置 {@link #sql(String)}，再按需追加条件/排序，最后执行 {@code list/one/page}。
 * 排序建议使用 alias 白名单接口，避免直接暴露原始表达式。
 *
 * @author Corwin 2026/4/9
 */
public interface XNativeQuery<R> {

    /**
     * 设置基础 SELECT SQL 模板。
     */
    XNativeQuery<R> sql(String sql);

    /**
     * 显式设置统计 SQL 模板；未设置时使用自动包装统计。
     */
    XNativeQuery<R> countSql(String countSql);

    /**
     * 添加等值条件。
     */
    XNativeQuery<R> eq(String columnExpr, Object value);

    /**
     * 条件成立时添加等值条件。
     */
    XNativeQuery<R> eqIf(boolean condition, String columnExpr, Object value);

    /**
     * 条件成立时添加 like 条件。
     */
    XNativeQuery<R> likeIf(boolean condition, String columnExpr, String value);

    /**
     * 条件成立时添加 in 条件。
     */
    XNativeQuery<R> inIf(boolean condition, String columnExpr, Collection<?> values);

    /**
     * 条件成立时添加 between 条件。
     */
    XNativeQuery<R> betweenIf(boolean condition, String columnExpr, Object start, Object end);

    /**
     * 条件成立时添加大于等于条件。
     */
    XNativeQuery<R> geIf(boolean condition, String columnExpr, Object value);

    /**
     * 条件成立时添加小于等于条件。
     */
    XNativeQuery<R> leIf(boolean condition, String columnExpr, Object value);

    /**
     * 条件成立时添加 is null 条件。
     */
    XNativeQuery<R> isNullIf(boolean condition, String columnExpr);

    /**
     * 条件成立时添加 is not null 条件。
     */
    XNativeQuery<R> isNotNullIf(boolean condition, String columnExpr);

    /**
     * 按 SELECT alias 排序。
     */
    XNativeQuery<R> orderByAlias(String alias, XSortDirection direction);

    /**
     * 条件成立时按 alias 排序。
     */
    XNativeQuery<R> orderByAliasIf(boolean condition, String alias, XSortDirection direction);

    /**
     * 执行列表查询。
     */
    List<R> list();

    /**
     * 执行单条查询。
     *
     * @return 0 条返回 null，超过 1 条抛异常
     */
    R one();

    /**
     * 执行分页查询。
     */
    PageData<R> page(int pageNo, int pageSize);
}


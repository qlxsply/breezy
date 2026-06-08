package com.corwin.framework.xsql;

import com.corwin.framework.domain.page.PageData;

import java.util.Collection;
import java.util.List;

/**
 * 单表查询 DSL。
 * <p>
 * 字段参数统一使用实体属性路径（支持嵌套路径），不直接暴露数据库列名。
 * 同时提供两种写法：
 * <ul>
 *     <li>字符串属性路径（如 {@code "status"}、{@code "profile.nickName"}）</li>
 *     <li>Lambda Getter 引用（如 {@code User::getStatus}）</li>
 * </ul>
 * <p>
 * 所有 {@code *If} 方法均遵循“条件为 false 则忽略，不改动当前查询状态”的约定。
 *
 * @author Corwin 2026/4/9
 */
public interface XTableQuery<E, R> {

    /**
     * 添加等值条件。
     */
    XTableQuery<E, R> eq(String propertyPath, Object value);

    XTableQuery<E, R> eq(XGetter<E, ?> getter, Object value);

    /**
     * 条件成立时添加等值条件。
     */
    XTableQuery<E, R> eqIf(boolean condition, String propertyPath, Object value);

    XTableQuery<E, R> eqIf(boolean condition, XGetter<E, ?> getter, Object value);

    /**
     * 添加不等值条件。
     */
    XTableQuery<E, R> ne(String propertyPath, Object value);

    XTableQuery<E, R> ne(XGetter<E, ?> getter, Object value);

    /**
     * 条件成立时添加不等值条件。
     */
    XTableQuery<E, R> neIf(boolean condition, String propertyPath, Object value);

    XTableQuery<E, R> neIf(boolean condition, XGetter<E, ?> getter, Object value);

    /**
     * 添加大于条件。
     */
    XTableQuery<E, R> gt(String propertyPath, Object value);

    XTableQuery<E, R> gt(XGetter<E, ?> getter, Object value);

    /**
     * 条件成立时添加大于条件。
     */
    XTableQuery<E, R> gtIf(boolean condition, String propertyPath, Object value);

    XTableQuery<E, R> gtIf(boolean condition, XGetter<E, ?> getter, Object value);

    /**
     * 添加小于条件。
     */
    XTableQuery<E, R> lt(String propertyPath, Object value);

    XTableQuery<E, R> lt(XGetter<E, ?> getter, Object value);

    /**
     * 条件成立时添加小于条件。
     */
    XTableQuery<E, R> ltIf(boolean condition, String propertyPath, Object value);

    XTableQuery<E, R> ltIf(boolean condition, XGetter<E, ?> getter, Object value);

    /**
     * 添加大于等于条件。
     */
    XTableQuery<E, R> ge(String propertyPath, Object value);

    XTableQuery<E, R> ge(XGetter<E, ?> getter, Object value);

    /**
     * 条件成立时添加大于等于条件。
     */
    XTableQuery<E, R> geIf(boolean condition, String propertyPath, Object value);

    XTableQuery<E, R> geIf(boolean condition, XGetter<E, ?> getter, Object value);

    /**
     * 添加小于等于条件。
     */
    XTableQuery<E, R> le(String propertyPath, Object value);

    XTableQuery<E, R> le(XGetter<E, ?> getter, Object value);

    /**
     * 条件成立时添加小于等于条件。
     */
    XTableQuery<E, R> leIf(boolean condition, String propertyPath, Object value);

    XTableQuery<E, R> leIf(boolean condition, XGetter<E, ?> getter, Object value);

    /**
     * 添加双侧模糊匹配条件（%value%）。
     */
    XTableQuery<E, R> like(String propertyPath, String value);

    XTableQuery<E, R> like(XGetter<E, ?> getter, String value);

    /**
     * 条件成立时添加双侧模糊匹配条件。
     */
    XTableQuery<E, R> likeIf(boolean condition, String propertyPath, String value);

    XTableQuery<E, R> likeIf(boolean condition, XGetter<E, ?> getter, String value);

    /**
     * 添加左模糊条件（%value）。
     */
    XTableQuery<E, R> leftLike(String propertyPath, String value);

    XTableQuery<E, R> leftLike(XGetter<E, ?> getter, String value);

    /**
     * 条件成立时添加左模糊条件。
     */
    XTableQuery<E, R> leftLikeIf(boolean condition, String propertyPath, String value);

    XTableQuery<E, R> leftLikeIf(boolean condition, XGetter<E, ?> getter, String value);

    /**
     * 添加右模糊条件（value%）。
     */
    XTableQuery<E, R> rightLike(String propertyPath, String value);

    XTableQuery<E, R> rightLike(XGetter<E, ?> getter, String value);

    /**
     * 条件成立时添加右模糊条件。
     */
    XTableQuery<E, R> rightLikeIf(boolean condition, String propertyPath, String value);

    XTableQuery<E, R> rightLikeIf(boolean condition, XGetter<E, ?> getter, String value);

    /**
     * 添加 in 条件。
     */
    XTableQuery<E, R> in(String propertyPath, Collection<?> values);

    XTableQuery<E, R> in(XGetter<E, ?> getter, Collection<?> values);

    /**
     * 条件成立时添加 in 条件。
     */
    XTableQuery<E, R> inIf(boolean condition, String propertyPath, Collection<?> values);

    XTableQuery<E, R> inIf(boolean condition, XGetter<E, ?> getter, Collection<?> values);

    /**
     * 添加 not in 条件。
     */
    XTableQuery<E, R> notIn(String propertyPath, Collection<?> values);

    XTableQuery<E, R> notIn(XGetter<E, ?> getter, Collection<?> values);

    /**
     * 条件成立时添加 not in 条件。
     */
    XTableQuery<E, R> notInIf(boolean condition, String propertyPath, Collection<?> values);

    XTableQuery<E, R> notInIf(boolean condition, XGetter<E, ?> getter, Collection<?> values);

    /**
     * 添加 between 条件。
     */
    XTableQuery<E, R> between(String propertyPath, Object start, Object end);

    XTableQuery<E, R> between(XGetter<E, ?> getter, Object start, Object end);

    /**
     * 条件成立时添加 between 条件。
     */
    XTableQuery<E, R> betweenIf(boolean condition, String propertyPath, Object start, Object end);

    XTableQuery<E, R> betweenIf(boolean condition, XGetter<E, ?> getter, Object start, Object end);

    /**
     * 添加 not between 条件。
     */
    XTableQuery<E, R> notBetween(String propertyPath, Object start, Object end);

    XTableQuery<E, R> notBetween(XGetter<E, ?> getter, Object start, Object end);

    /**
     * 条件成立时添加 not between 条件。
     */
    XTableQuery<E, R> notBetweenIf(boolean condition, String propertyPath, Object start, Object end);

    XTableQuery<E, R> notBetweenIf(boolean condition, XGetter<E, ?> getter, Object start, Object end);

    /**
     * 添加 is null 条件。
     */
    XTableQuery<E, R> isNull(String propertyPath);

    XTableQuery<E, R> isNull(XGetter<E, ?> getter);

    /**
     * 条件成立时添加 is null 条件。
     */
    XTableQuery<E, R> isNullIf(boolean condition, String propertyPath);

    XTableQuery<E, R> isNullIf(boolean condition, XGetter<E, ?> getter);

    /**
     * 添加 is not null 条件。
     */
    XTableQuery<E, R> isNotNull(String propertyPath);

    XTableQuery<E, R> isNotNull(XGetter<E, ?> getter);

    /**
     * 条件成立时添加 is not null 条件。
     */
    XTableQuery<E, R> isNotNullIf(boolean condition, String propertyPath);

    XTableQuery<E, R> isNotNullIf(boolean condition, XGetter<E, ?> getter);

    /**
     * 添加排序（属性路径必须在可排序白名单中）。
     */
    XTableQuery<E, R> orderBy(String propertyPath, XSortDirection direction);

    XTableQuery<E, R> orderBy(XGetter<E, ?> getter, XSortDirection direction);

    /**
     * 条件成立时添加排序。
     */
    XTableQuery<E, R> orderByIf(boolean condition, String propertyPath, XSortDirection direction);

    XTableQuery<E, R> orderByIf(boolean condition, XGetter<E, ?> getter, XSortDirection direction);

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

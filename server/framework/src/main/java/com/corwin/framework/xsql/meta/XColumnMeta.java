package com.corwin.framework.xsql.meta;

import com.corwin.framework.xsql.codec.XValueCodec;

/**
 * 列元数据。
 * <p>
 * 描述属性路径到数据库列的映射关系，以及该列在查询中的能力与编解码策略。
 *
 * @author Corwin 2026/4/9
 */
public record XColumnMeta(
        String propertyPath,
        String columnName,
        String selectAlias,
        Class<?> javaType,
        XValueCodec valueCodec,
        boolean selectable,
        boolean conditionable,
        boolean sortable
) {
}


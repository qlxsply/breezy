package com.corwin.framework.xsql.meta;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单表实体元数据。
 * <p>
 * 该对象是单表查询的“预编译结果”，包含：
 * 物理表信息、基础 select/count SQL、属性路径与列映射以及别名映射。
 *
 * @author Corwin 2026/4/9
 */
public final class XEntityMeta {

    private final Class<?> entityClass;
    private final String tableName;
    private final String tableAlias;
    private final String baseSelectSql;
    private final String baseCountSql;
    private final Map<String, XColumnMeta> selectableColumns;
    private final Map<String, XColumnMeta> conditionableColumns;
    private final Map<String, XColumnMeta> sortableColumns;
    private final Map<String, XColumnMeta> aliasColumns;

    public XEntityMeta(Class<?> entityClass, String tableName, String tableAlias, String baseSelectSql,
            String baseCountSql, Map<String, XColumnMeta> selectableColumns, Map<String, XColumnMeta> conditionableColumns,
            Map<String, XColumnMeta> sortableColumns, Map<String, XColumnMeta> aliasColumns) {
        this.entityClass = entityClass;
        this.tableName = tableName;
        this.tableAlias = tableAlias;
        this.baseSelectSql = baseSelectSql;
        this.baseCountSql = baseCountSql;
        this.selectableColumns = Collections.unmodifiableMap(new LinkedHashMap<>(selectableColumns));
        this.conditionableColumns = Collections.unmodifiableMap(new LinkedHashMap<>(conditionableColumns));
        this.sortableColumns = Collections.unmodifiableMap(new LinkedHashMap<>(sortableColumns));
        this.aliasColumns = Collections.unmodifiableMap(new LinkedHashMap<>(aliasColumns));
    }

    /**
     * 实体类型。
     */
    public Class<?> entityClass() {
        return entityClass;
    }

    /**
     * 物理表名。
     */
    public String tableName() {
        return tableName;
    }

    /**
     * SQL 中使用的固定表别名。
     */
    public String tableAlias() {
        return tableAlias;
    }

    /**
     * 预编译基础 select SQL（不含 where/order/pagination）。
     */
    public String baseSelectSql() {
        return baseSelectSql;
    }

    /**
     * 预编译基础 count SQL（不含 where）。
     */
    public String baseCountSql() {
        return baseCountSql;
    }

    /**
     * 可返回列映射。
     */
    public Map<String, XColumnMeta> selectableColumns() {
        return selectableColumns;
    }

    /**
     * 可条件列映射。
     */
    public Map<String, XColumnMeta> conditionableColumns() {
        return conditionableColumns;
    }

    /**
     * 可排序列映射。
     */
    public Map<String, XColumnMeta> sortableColumns() {
        return sortableColumns;
    }

    /**
     * select alias 到列元数据映射。
     */
    public Map<String, XColumnMeta> aliasColumns() {
        return aliasColumns;
    }
}


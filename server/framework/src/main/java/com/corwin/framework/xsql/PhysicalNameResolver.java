package com.corwin.framework.xsql;

/**
 * 物理命名解析器。
 * <p>
 * 用于将实体逻辑名解析为数据库真实表名/列名，解决命名策略差异问题。
 * <p>
 * 约定：
 * <ul>
 *     <li>{@code logicalTableName} / {@code logicalColumnName} 表示实体映射中显式声明的逻辑名，未显式声明时可为 {@code null}。</li>
 *     <li>实现类需要同时处理显式命名与默认命名两种场景，并返回最终可直接用于 SQL 的物理名。</li>
 * </ul>
 *
 * @author Corwin 2026/4/9
 */
public interface PhysicalNameResolver {

    /**
     * 解析实体对应的物理表名。
     *
     * @param entityClass 实体类型
     * @param logicalTableName 显式逻辑表名；未显式声明时允许为 {@code null}
     * @return 可直接用于 SQL 的物理表名
     */
    String resolveTableName(Class<?> entityClass, String logicalTableName);

    /**
     * 解析属性路径对应的物理列名。
     *
     * @param ownerType 所属类型（实体或 embeddable）
     * @param propertyPath 属性路径，如 {@code profile.nickName}
     * @param logicalColumnName 显式逻辑列名；未显式声明时允许为 {@code null}
     * @return 可直接用于 SQL 的物理列名
     */
    String resolveColumnName(Class<?> ownerType, String propertyPath, String logicalColumnName);
}


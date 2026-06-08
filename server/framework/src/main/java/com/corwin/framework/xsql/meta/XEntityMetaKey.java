package com.corwin.framework.xsql.meta;

/**
 * 元数据缓存 Key。
 * <p>
 * 由数据源身份、实体类型、命名解析器身份共同组成，避免不同环境误复用元数据。
 *
 * @author Corwin 2026/4/9
 */
public record XEntityMetaKey(
        int dataSourceIdentity,
        Class<?> entityClass,
        int resolverIdentity
) {
}


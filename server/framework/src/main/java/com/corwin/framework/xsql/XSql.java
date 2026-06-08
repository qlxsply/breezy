package com.corwin.framework.xsql;

import javax.sql.DataSource;

/**
 * XSql 全局入口。
 * <p>
 * 该对象是无状态门面，负责创建与数据源绑定的 {@link XSqlSession}。
 * 组件本身只读，不参与 JPA 实体托管。
 *
 * @author Corwin 2026/4/9
 */
public interface XSql {

    /**
     * 创建构建器。
     */
    static Builder builder() {
        return DefaultXSql.builder();
    }

    /**
     * 使用指定数据源创建会话。
     *
     * @param dataSource 目标数据源
     * @return 绑定数据源的查询会话
     */
    XSqlSession using(DataSource dataSource);

    /**
     * XSql 构建器。
     */
    interface Builder {

        /**
         * 指定物理命名解析器。
         */
        Builder physicalNameResolver(PhysicalNameResolver resolver);

        /**
         * 构建 XSql 实例。
         */
        XSql build();
    }
}


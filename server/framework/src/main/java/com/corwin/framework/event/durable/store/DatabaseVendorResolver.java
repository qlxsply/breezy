package com.corwin.framework.event.durable.store;

import javax.sql.DataSource;

/**
 * 数据库类型识别器。
 *
 * @author Corwin 2026/4/12
 */
public interface DatabaseVendorResolver {

    /**
     * 基于数据源识别数据库类型。
     *
     * @param dataSource 数据源
     * @return 数据库类型
     */
    DatabaseVendor resolve(DataSource dataSource);
}


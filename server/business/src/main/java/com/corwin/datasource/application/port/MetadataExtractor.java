package com.corwin.datasource.application.port;

import com.corwin.datasource.domain.model.DatabaseSource;

import java.util.List;

/**
 * @author Corwin 2026/1/29
 */
public interface MetadataExtractor {

    /**
     * 获取数据源层级的基础元数据
     */
    MetadataExtractResult.BaseInfo getBaseInfo(DatabaseSource dataSource);

    /**
     * 列出数据源下可用的数据库（Catalogs 或 Schemas）
     */
    List<String> listDatabases(DatabaseSource dataSource);

    /**
     * 提取指定数据库的表和列信息
     */
    MetadataExtractResult extract(DatabaseSource dataSource, String databaseName);

    /**
     * 仅测试连接是否可用
     */
    void testConnection(DatabaseSource dataSource);
}

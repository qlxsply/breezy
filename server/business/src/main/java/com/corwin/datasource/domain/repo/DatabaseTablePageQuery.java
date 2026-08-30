package com.corwin.datasource.domain.repo;

/**
 * 数据表分页查询条件。
 *
 * @author Corwin 2026/4/15
 */
public record DatabaseTablePageQuery(
    Long databaseId, String tableSchema, String tableNameLike, String tableType) {}

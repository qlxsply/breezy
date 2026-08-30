package com.corwin.datasource.domain.repo;

import com.corwin.datasource.domain.model.DatabaseType;

/**
 * 数据源分页查询条件。
 *
 * @author Corwin 2026/4/15
 */
public record DatabaseSourcePageQuery(DatabaseType dbType, String nameLike) {}

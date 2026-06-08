package com.corwin.datasource.domain.repo;

import com.corwin.datasource.domain.model.DatabaseTable;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.repo.DynamicPageQueryRepository;
import com.corwin.framework.domain.repo.DomainRepository;

import java.util.List;

/**
 *
 * @author Corwin 2026/1/11
 */
public interface DatabaseTableRepository extends DomainRepository<DatabaseTable, Long>,
        DynamicPageQueryRepository<DatabaseTable, DatabaseTablePageQuery> {

    void deleteByDatabaseId(Long databaseId);

    List<DatabaseTable> findByDatabaseId(Long databaseId);

    PageData<DatabaseTable> findByDatabaseId(Long databaseId, PageSpec spec);

    PageData<DatabaseTable> findByDatabaseIdAndTableSchema(Long databaseId, String tableSchema, PageSpec spec);

    PageData<DatabaseTable> findByDatabaseIdAndTableNameContainingIgnoreCase(Long databaseId, String nameLike,
            PageSpec spec);

    PageData<DatabaseTable> findByDatabaseIdAndTableSchemaAndTableNameContainingIgnoreCase(Long databaseId,
            String tableSchema, String nameLike, PageSpec spec);

    PageData<DatabaseTable> findByDatabaseIdAndTableType(Long databaseId, String tableType, PageSpec spec);

    PageData<DatabaseTable> findByDatabaseIdAndTableSchemaAndTableType(Long databaseId, String tableSchema,
            String tableType, PageSpec spec);
}

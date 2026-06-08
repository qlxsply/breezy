package com.corwin.datasource.domain.repo;

import com.corwin.datasource.domain.model.DatabaseColumn;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.repo.DomainRepository;

import java.util.List;

/**
 *
 * @author Corwin 2026/1/11
 */
public interface DatabaseColumnRepository extends DomainRepository<DatabaseColumn, Long> {

    List<DatabaseColumn> findByTableId(Long tableId);

    PageData<DatabaseColumn> findByTableId(Long tableId, PageSpec spec);

    PageData<DatabaseColumn> findByTableIdAndColumnNameContainingIgnoreCase(Long tableId, String nameLike,
            PageSpec spec);

    void deleteByDatabaseId(Long databaseId);

    void deleteByTableId(Long tableId);
}

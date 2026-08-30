package com.corwin.datasource.domain.repo;

import com.corwin.datasource.domain.model.DatabaseTable;
import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.framework.domain.repo.DynamicPageQueryRepository;
import java.util.List;

/**
 * @author Corwin 2026/1/11
 */
public interface DatabaseTableRepository
    extends DomainRepository<DatabaseTable, Long>,
        DynamicPageQueryRepository<DatabaseTable, DatabaseTablePageQuery> {

  void deleteByDatabaseId(Long databaseId);

  List<DatabaseTable> findByDatabaseId(Long databaseId);
}

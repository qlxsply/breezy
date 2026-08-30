package com.corwin.datasource.domain.repo;

import com.corwin.datasource.domain.model.DatabaseSchema;
import com.corwin.framework.domain.repo.DomainRepository;
import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/1/11
 */
public interface DatabaseSchemaRepository extends DomainRepository<DatabaseSchema, Long> {

  List<DatabaseSchema> findAll();

  List<DatabaseSchema> findByDataSourceId(Long dataSourceId);

  List<DatabaseSchema> findByDataSourceIdIsNull();

  Optional<DatabaseSchema> findByDataSourceIdAndDatabaseName(
      Long dataSourceId, String databaseName);

  int clearDataSourceIdByDataSourceId(Long dataSourceId);
}

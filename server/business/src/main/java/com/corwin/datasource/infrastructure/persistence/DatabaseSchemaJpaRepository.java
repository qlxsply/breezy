package com.corwin.datasource.infrastructure.persistence;

import com.corwin.datasource.domain.model.DatabaseSchema;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * @author Corwin 2026/1/11
 */
public interface DatabaseSchemaJpaRepository extends JpaRepository<DatabaseSchema, Long> {

  List<DatabaseSchema> findByDataSourceId(Long dataSourceId);

  List<DatabaseSchema> findByDataSourceIdIsNull();

  Optional<DatabaseSchema> findByDataSourceIdAndDatabaseName(
      Long dataSourceId, String databaseName);

  @Modifying
  @Query(
      "update DatabaseSchema db set db.dataSourceId = null where db.dataSourceId = :dataSourceId")
  int clearDataSourceIdByDataSourceId(@Param("dataSourceId") Long dataSourceId);
}

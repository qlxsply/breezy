package com.corwin.datasource.infrastructure.persistence;

import com.corwin.datasource.domain.model.DatabaseTable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Corwin 2026/1/11
 */
public interface DatabaseTableJpaRepository extends JpaRepository<DatabaseTable, Long> {

    void deleteByDataSourceId(Long dataSourceId);

    void deleteByDatabaseId(Long databaseId);

    List<DatabaseTable> findByDatabaseId(Long databaseId);

    Page<DatabaseTable> findByDatabaseId(Long databaseId, Pageable pageable);

    Page<DatabaseTable> findByDatabaseIdAndTableSchema(Long databaseId, String tableSchema, Pageable pageable);

    Page<DatabaseTable> findByDatabaseIdAndTableNameContainingIgnoreCase(Long databaseId, String nameLike,
            Pageable pageable);

    Page<DatabaseTable> findByDatabaseIdAndTableSchemaAndTableNameContainingIgnoreCase(Long databaseId,
            String tableSchema, String nameLike, Pageable pageable);

    Page<DatabaseTable> findByDatabaseIdAndTableType(Long databaseId, String tableType, Pageable pageable);

    Page<DatabaseTable> findByDatabaseIdAndTableSchemaAndTableType(Long databaseId, String tableSchema,
            String tableType, Pageable pageable);
}

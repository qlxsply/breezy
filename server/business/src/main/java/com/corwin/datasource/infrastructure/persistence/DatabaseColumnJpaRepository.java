package com.corwin.datasource.infrastructure.persistence;

import com.corwin.datasource.domain.model.DatabaseColumn;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Corwin 2026/1/11
 */
public interface DatabaseColumnJpaRepository extends JpaRepository<DatabaseColumn, Long> {

  List<DatabaseColumn> findByTableId(Long tableId);

  Page<DatabaseColumn> findByTableId(Long tableId, Pageable pageable);

  Page<DatabaseColumn> findByTableIdAndColumnNameContainingIgnoreCase(
      Long tableId, String nameLike, Pageable pageable);

  void deleteByDatabaseId(Long databaseId);

  void deleteByTableId(Long tableId);
}

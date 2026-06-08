package com.corwin.schemaforge.domain.repo;

import com.corwin.schemaforge.domain.model.DatabaseDdl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * @author Corwin 2026/2/24
 */
public interface DatabaseDdlRepository extends JpaRepository<DatabaseDdl, String>, JpaSpecificationExecutor<DatabaseDdl> {
    Page<DatabaseDdl> findByManagedDatabaseId(Long managedDatabaseId, Pageable pageable);
}

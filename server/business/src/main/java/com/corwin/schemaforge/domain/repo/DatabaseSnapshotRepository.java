package com.corwin.schemaforge.domain.repo;

import com.corwin.schemaforge.domain.model.DatabaseSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * @author Corwin 2026/2/24
 */
public interface DatabaseSnapshotRepository extends JpaRepository<DatabaseSnapshot, String>, JpaSpecificationExecutor<DatabaseSnapshot> {
    Page<DatabaseSnapshot> findByManagedDatabaseId(Long managedDatabaseId, Pageable pageable);
}

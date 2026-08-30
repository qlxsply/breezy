package com.corwin.schemaforge.domain.repo;

import com.corwin.schemaforge.domain.model.DatabaseSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Corwin 2026/2/24
 */
public interface DatabaseSnapshotRepository extends JpaRepository<DatabaseSnapshot, String> {}

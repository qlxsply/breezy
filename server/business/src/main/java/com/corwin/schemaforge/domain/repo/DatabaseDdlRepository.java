package com.corwin.schemaforge.domain.repo;

import com.corwin.schemaforge.domain.model.DatabaseDdl;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Corwin 2026/2/24
 */
public interface DatabaseDdlRepository extends JpaRepository<DatabaseDdl, String> {}

package com.corwin.datasource.infrastructure.persistence;

import com.corwin.datasource.domain.model.DatabaseSource;
import com.corwin.datasource.domain.model.DatabaseType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author Corwin 2026/1/11
 */
public interface DatabaseSourceJpaRepository extends JpaRepository<DatabaseSource, Long> {

    Optional<DatabaseSource> findByName(String name);

    Optional<DatabaseSource> findByDbTypeAndJdbcUrlAndUsername(DatabaseType dbType, String jdbcUrl, String username);

    Optional<DatabaseSource> findByAppDsKey(String appDsKey);
}

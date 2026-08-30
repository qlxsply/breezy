package com.corwin.datasource.domain.repo;

import com.corwin.datasource.domain.model.DatabaseSource;
import com.corwin.datasource.domain.model.DatabaseType;
import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.framework.domain.repo.DynamicPageQueryRepository;
import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/1/11
 */
public interface DatabaseSourceRepository
    extends DomainRepository<DatabaseSource, Long>,
        DynamicPageQueryRepository<DatabaseSource, DatabaseSourcePageQuery> {

  Optional<DatabaseSource> findByName(String name);

  Optional<DatabaseSource> findByDbTypeAndJdbcUrlAndUsername(
      DatabaseType dbType, String jdbcUrl, String username);

  Optional<DatabaseSource> findByAppDsKey(String appDsKey);

  List<DatabaseSource> findAll();
}

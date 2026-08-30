package com.corwin.bootstrap.application.service;

import com.corwin.bootstrap.application.DatabaseType;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * @author Corwin 2026/4/28
 */
@Component
@RequiredArgsConstructor
public class BootstrapDatabaseTypeResolver {

  private final Environment environment;

  public DatabaseType resolveCurrentDatabaseType() {
    String jdbcUrl = environment.getProperty("spring.datasource.url");
    DatabaseType databaseType = DatabaseType.fromJdbcUrl(jdbcUrl);
    if (databaseType == null) {
      throw new IllegalStateException("Cannot resolve database type from JDBC URL: " + jdbcUrl);
    }
    return databaseType;
  }
}

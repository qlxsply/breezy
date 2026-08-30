package com.corwin.datasource.application.port;

import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * @author Corwin 2026/2/7
 */
public interface AppDataSourceProvider {
  Map<String, DataSource> listAll();

  Optional<DataSource> findByKey(String key);

  default Optional<String> findPasswordByKey(String key) {
    return Optional.empty();
  }
}

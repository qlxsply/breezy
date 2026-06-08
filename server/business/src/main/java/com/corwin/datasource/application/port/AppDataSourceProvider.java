package com.corwin.datasource.application.port;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Optional;

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

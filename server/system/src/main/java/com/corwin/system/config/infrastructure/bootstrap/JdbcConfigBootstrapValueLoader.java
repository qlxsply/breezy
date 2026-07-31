package com.corwin.system.config.infrastructure.bootstrap;

import com.corwin.framework.config.bootstrap.ConfigBootstrapValueLoader;
import com.corwin.framework.config.bootstrap.RawConfigValue;
import com.corwin.framework.config.error.ConfigLoadException;
import org.springframework.core.env.ConfigurableEnvironment;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * @author Corwin 2026/7/30
 */
public final class JdbcConfigBootstrapValueLoader implements ConfigBootstrapValueLoader {

    private static final String TABLE_NAME = "sys_config_value";

    private static final String LOAD_SQL = """
            select config_key, content, schema_version, revision, configured
            from sys_config_value
            """;

    @Override
    public Map<String, RawConfigValue> load(ConfigurableEnvironment environment) {
        String url = firstNonBlank(environment.getProperty("spring.datasource.url"),
                environment.getProperty("bootstrap.datasource.url"));
        if (url == null) {
            throw new ConfigLoadException("Datasource URL is required before loading unified configs");
        }

        loadDriver(firstNonBlank(environment.getProperty("spring.datasource.driver-class-name"),
                environment.getProperty("bootstrap.datasource.driver-class-name")));

        Properties properties = connectionProperties(environment);
        try (Connection connection = DriverManager.getConnection(url, properties)) {
            if (!tableExists(connection)) {
                throw new ConfigLoadException(
                        "Required table sys_config_value does not exist; run bootstrap schema initialization first");
            }
            return loadValues(connection);
        } catch (SQLException ex) {
            throw new ConfigLoadException("Failed to load unified configs from database", ex);
        }
    }

    private Map<String, RawConfigValue> loadValues(Connection connection) throws SQLException {
        var values = new LinkedHashMap<String, RawConfigValue>();
        try (PreparedStatement statement = connection.prepareStatement(LOAD_SQL);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                var value = new RawConfigValue(resultSet.getString("config_key"), resultSet.getString("content"),
                        resultSet.getInt("schema_version"), resultSet.getLong("revision"),
                        resultSet.getBoolean("configured"));
                if (values.putIfAbsent(value.configKey(), value) != null) {
                    throw new ConfigLoadException("Duplicate persisted config key: " + value.configKey());
                }
            }
        }
        return Map.copyOf(values);
    }

    private boolean tableExists(Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        String catalog = connection.getCatalog();
        String schema = connection.getSchema();
        for (String candidate : tableNameCandidates()) {
            if (tableExists(metadata, catalog, schema, candidate) || schema != null && tableExists(metadata, catalog,
                    null, candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean tableExists(DatabaseMetaData metadata, String catalog, String schema, String tableName) throws
            SQLException {
        try (ResultSet tables = metadata.getTables(catalog, schema, tableName, new String[]{"TABLE"})) {
            return tables.next();
        }
    }

    private String[] tableNameCandidates() {
        return new String[]{TABLE_NAME, TABLE_NAME.toUpperCase(Locale.ROOT), toTitleCase(TABLE_NAME)};
    }

    private Properties connectionProperties(ConfigurableEnvironment environment) {
        var properties = new Properties();
        String username = firstNonBlank(environment.getProperty("spring.datasource.username"),
                environment.getProperty("bootstrap.datasource.username"));
        String password = firstNonBlank(environment.getProperty("spring.datasource.password"),
                environment.getProperty("bootstrap.datasource.password"));
        if (username != null) {
            properties.setProperty("user", username);
        }
        if (password != null) {
            properties.setProperty("password", password);
        }
        return properties;
    }

    private void loadDriver(String driverClassName) {
        if (driverClassName == null) {
            return;
        }
        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException ex) {
            throw new ConfigLoadException("Datasource driver class not found: " + driverClassName, ex);
        }
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }

    private String toTitleCase(String value) {
        String[] parts = value.split("_");
        var result = new StringBuilder();
        for (int index = 0; index < parts.length; index++) {
            if (index > 0) {
                result.append('_');
            }
            String part = parts[index];
            result.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            result.append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return result.toString();
    }
}

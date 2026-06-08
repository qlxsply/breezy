package com.corwin.framework.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Loads active database configs from sys_config during Spring Boot startup
 * and registers them into ConfigRegistry.
 *
 * @author Corwin 2026/1/31
 */
@Slf4j
public class DatabaseConfigRegistryEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String CONFIG_TABLE_NAME = "sys_config";

    private static final String LOAD_CONFIG_SQL = """
            select code, config_value, config_value_type
            from sys_config
            where expired = false
            """;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String initialization = environment.getProperty("bootstrap.initialization.process");
        if ("true".equalsIgnoreCase(initialization)) {
            log.info("Skip loading database configs because bootstrap initialization process is enabled");
            return;
        }

        String url = firstNonBlank(environment.getProperty("spring.datasource.url"),
                environment.getProperty("bootstrap.datasource.url"));
        if (url == null) {
            log.info("Skip loading database configs because datasource url is not configured");
            return;
        }

        String username = firstNonBlank(environment.getProperty("spring.datasource.username"),
                environment.getProperty("bootstrap.datasource.username"));

        String password = firstNonBlank(environment.getProperty("spring.datasource.password"),
                environment.getProperty("bootstrap.datasource.password"));

        List<ConfigItem> configItems = loadConfigItems(url, username, password);

        if (configItems.isEmpty()) {
            log.info("No active database configs loaded from sys_config");
            return;
        }

        ConfigRegistry.initialize(configItems);

        log.info("Loaded {} active configs from sys_config into ConfigRegistry", configItems.size());
    }

    private List<ConfigItem> loadConfigItems(String url, String username, String password) {
        List<ConfigItem> items = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            if (!hasSysConfigTable(connection)) {
                log.info("Skip loading database configs because sys_config table does not exist yet");
                return items;
            }

            try (PreparedStatement statement = connection.prepareStatement(LOAD_CONFIG_SQL);
                    ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    ConfigItem item = new ConfigItem();
                    item.key = resultSet.getString("code");
                    item.value = resultSet.getString("config_value");
                    item.type = parseConfigValueType(resultSet.getString("config_value_type"));
                    items.add(item);
                }
            }

            return items;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load database configs from sys_config", ex);
        }
    }

    private boolean hasSysConfigTable(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();

        return tableExists(metaData, CONFIG_TABLE_NAME) || tableExists(metaData,
                CONFIG_TABLE_NAME.toUpperCase(Locale.ROOT)) || tableExists(metaData, toTitleCase(CONFIG_TABLE_NAME));
    }

    private boolean tableExists(DatabaseMetaData metaData, String tableName) throws SQLException {
        try (ResultSet resultSet = metaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return resultSet.next();
        }
    }

    private ConfigValueType parseConfigValueType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return ConfigValueType.STR;
        }

        return ConfigValueType.valueOf(rawType.trim().toUpperCase(Locale.ROOT));
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isBlank()) {
            return first;
        }
        if (second != null && !second.trim().isBlank()) {
            return second;
        }
        return null;
    }

    private String toTitleCase(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        String[] parts = value.split("_");
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                builder.append("_");
            }

            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }

            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }

        return builder.toString();
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }

}

package com.corwin.bootstrap.application;

import java.util.Locale;

/**
 * @author Corwin 2026/4/28
 */
public enum DatabaseType {
    MYSQL,
    POSTGRESQL;

    public static DatabaseType fromJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return null;
        }
        String normalized = jdbcUrl.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("jdbc:mysql:")) {
            return MYSQL;
        }
        if (normalized.startsWith("jdbc:postgresql:")) {
            return POSTGRESQL;
        }
        return null;
    }
}

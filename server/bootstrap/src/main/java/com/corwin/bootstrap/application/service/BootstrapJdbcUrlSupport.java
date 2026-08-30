package com.corwin.bootstrap.application.service;

import com.corwin.bootstrap.application.DatabaseType;

/**
 * @author Corwin 2026/4/24
 */
public final class BootstrapJdbcUrlSupport {

  private BootstrapJdbcUrlSupport() {}

  public static String toFolder(DatabaseType databaseType) {
    return switch (databaseType) {
      case MYSQL -> "mysql";
      case POSTGRESQL -> "pgsql";
    };
  }

  public static String resolveDriverClassName(DatabaseType databaseType, String configuredDriver) {
    if (configuredDriver != null && !configuredDriver.isBlank()) {
      return configuredDriver.trim();
    }
    return switch (databaseType) {
      case MYSQL -> "com.mysql.cj.jdbc.Driver";
      case POSTGRESQL -> "org.postgresql.Driver";
    };
  }

  public static String extractDatabaseName(DatabaseType databaseType, String jdbcUrl) {
    if (jdbcUrl == null || jdbcUrl.isBlank()) {
      return null;
    }
    return switch (databaseType) {
      case MYSQL, POSTGRESQL -> extractPathDatabaseName(jdbcUrl);
    };
  }

  public static String buildAdminUrl(DatabaseType databaseType, String jdbcUrl) {
    return switch (databaseType) {
      case MYSQL -> buildMysqlAdminUrl(jdbcUrl);
      case POSTGRESQL -> buildPostgresqlAdminUrl(jdbcUrl);
    };
  }

  public static String quoteIdentifier(DatabaseType databaseType, String identifier) {
    if (identifier == null || identifier.isBlank()) {
      throw new IllegalArgumentException("Identifier must not be blank");
    }
    return switch (databaseType) {
      case MYSQL -> "`" + identifier.replace("`", "``") + "`";
      case POSTGRESQL -> "\"" + identifier.replace("\"", "\"\"") + "\"";
    };
  }

  private static String extractPathDatabaseName(String jdbcUrl) {
    int queryIndex = jdbcUrl.indexOf('?');
    String base = queryIndex >= 0 ? jdbcUrl.substring(0, queryIndex) : jdbcUrl;
    int slashIndex = base.lastIndexOf('/');
    if (slashIndex < 0 || slashIndex == base.length() - 1) {
      return null;
    }
    return base.substring(slashIndex + 1);
  }

  private static String buildMysqlAdminUrl(String jdbcUrl) {
    int queryIndex = jdbcUrl.indexOf('?');
    String query = queryIndex >= 0 ? jdbcUrl.substring(queryIndex) : "";
    String base = queryIndex >= 0 ? jdbcUrl.substring(0, queryIndex) : jdbcUrl;
    int slashIndex = base.lastIndexOf('/');
    if (slashIndex < "jdbc:mysql://".length()) {
      return jdbcUrl;
    }
    return base.substring(0, slashIndex) + query;
  }

  private static String buildPostgresqlAdminUrl(String jdbcUrl) {
    int queryIndex = jdbcUrl.indexOf('?');
    String query = queryIndex >= 0 ? jdbcUrl.substring(queryIndex) : "";
    String base = queryIndex >= 0 ? jdbcUrl.substring(0, queryIndex) : jdbcUrl;
    int slashIndex = base.lastIndexOf('/');
    if (slashIndex < "jdbc:postgresql://".length()) {
      return jdbcUrl;
    }
    return base.substring(0, slashIndex + 1) + "postgres" + query;
  }
}

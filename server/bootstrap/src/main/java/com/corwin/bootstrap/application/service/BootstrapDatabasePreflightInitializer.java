package com.corwin.bootstrap.application.service;

import com.corwin.bootstrap.application.DatabaseType;
import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * @author Corwin 2026/4/28
 */
public class BootstrapDatabasePreflightInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    ConfigurableEnvironment environment = applicationContext.getEnvironment();
    DataSourceSettings settings = promptForSettings();
    ensureDatabaseExists(settings);

    Map<String, Object> overrides = new LinkedHashMap<>();
    overrides.put("spring.datasource.url", settings.url());
    overrides.put("spring.datasource.username", settings.username());
    overrides.put("spring.datasource.password", settings.password());
    overrides.put("spring.datasource.driver-class-name", settings.driverClassName());
    overrides.put("spring.datasource.hikari.auto-commit", "false");
    environment
        .getPropertySources()
        .addFirst(new MapPropertySource("bootstrapConsoleDataSource", overrides));
  }

  private DataSourceSettings promptForSettings() {
    Scanner scanner = new Scanner(System.in);

    System.out.println("当前引导程序仅通过控制台录入数据库连接信息。");
    System.out.println("当前支持的数据库类型：");
    System.out.println("1. MySQL");
    System.out.println("2. pgsql");

    DatabaseType databaseType = selectDatabaseType(scanner);
    String host = readHost(scanner);
    int port = readPort(scanner, defaultPort(databaseType));
    String databaseName = readDatabase(scanner, "breezy");
    String username = readOptional(scanner, "请输入账号 [默认 " + defaultUsername(databaseType) + "]: ");
    String password = readPassword(scanner);

    if (username == null || username.isBlank()) {
      username = defaultUsername(databaseType);
    }

    String url = buildJdbcUrl(databaseType, host, port, databaseName);
    String driverClassName = BootstrapJdbcUrlSupport.resolveDriverClassName(databaseType, null);

    System.out.println("已生成连接信息: " + databaseType + " " + host + ":" + port + "/" + databaseName);
    return new DataSourceSettings(databaseType, url, username, password, driverClassName);
  }

  private void ensureDatabaseExists(DataSourceSettings settings) {
    String databaseName =
        BootstrapJdbcUrlSupport.extractDatabaseName(settings.databaseType(), settings.url());
    if (databaseName == null || databaseName.isBlank()) {
      return;
    }

    try {
      Class.forName(settings.driverClassName());
      String adminUrl =
          BootstrapJdbcUrlSupport.buildAdminUrl(settings.databaseType(), settings.url());
      boolean exists =
          BootstrapJdbcTransactionSupport.execute(
              () -> DriverManager.getConnection(adminUrl, settings.username(), settings.password()),
              connection -> databaseExists(connection.getMetaData(), databaseName));
      if (exists) {
        System.out.println("数据库已存在: " + databaseName);
        return;
      }

      String sql = loadCreateDatabaseSql(settings.databaseType(), databaseName);
      BootstrapJdbcTransactionSupport.executeNonTransactional(
          () -> DriverManager.getConnection(adminUrl, settings.username(), settings.password()),
          connection -> {
            try (Statement statement = connection.createStatement()) {
              statement.execute(sql);
            }
            return null;
          });
      System.out.println("已自动创建数据库: " + databaseName);
    } catch (Exception e) {
      throw new IllegalStateException("Bootstrap 数据库预处理失败: " + settings.url(), e);
    }
  }

  private boolean databaseExists(DatabaseMetaData metaData, String databaseName) throws Exception {
    try (ResultSet catalogs = metaData.getCatalogs()) {
      while (catalogs.next()) {
        String name = catalogs.getString(1);
        if (databaseName.equalsIgnoreCase(name)) {
          return true;
        }
      }
    }
    return false;
  }

  private String loadCreateDatabaseSql(DatabaseType databaseType, String databaseName)
      throws Exception {
    String path =
        "bootstarp/sql/" + BootstrapJdbcUrlSupport.toFolder(databaseType) + "/create_database.sql";
    try (var inputStream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
      if (inputStream == null) {
        throw new IllegalStateException("Create database SQL template not found: " + path);
      }
      String template = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
      return template.replace(
          "${databaseName}", BootstrapJdbcUrlSupport.quoteIdentifier(databaseType, databaseName));
    }
  }

  private DatabaseType selectDatabaseType(Scanner scanner) {
    while (true) {
      String input = readRequired(scanner, "请选择数据库类型");
      if ("1".equals(input) || "mysql".equalsIgnoreCase(input)) {
        return DatabaseType.MYSQL;
      }
      if ("2".equals(input)
          || "pgsql".equalsIgnoreCase(input)
          || "postgresql".equalsIgnoreCase(input)) {
        return DatabaseType.POSTGRESQL;
      }
      System.out.println("无效选项，请输入 1 或 2。");
    }
  }

  private String readHost(Scanner scanner) {
    String defaultHost = "127.0.0.1";
    String input = readOptional(scanner, "请输入 IP 或主机名 [默认 " + defaultHost + "]: ");
    if (input == null || input.isBlank()) {
      return defaultHost;
    }
    return input.trim();
  }

  private int readPort(Scanner scanner, int defaultPort) {
    while (true) {
      String input = readOptional(scanner, "请输入端口 [默认 " + defaultPort + "]: ");
      if (input == null || input.isBlank()) {
        return defaultPort;
      }
      try {
        return Integer.parseInt(input);
      } catch (NumberFormatException e) {
        System.out.println("端口必须是数字。");
      }
    }
  }

  private String readDatabase(Scanner scanner, String defaultDatabase) {
    String input = readOptional(scanner, "请输入数据库名 [默认 " + defaultDatabase + "]: ");
    if (input == null || input.isBlank()) {
      return defaultDatabase;
    }
    return input.trim();
  }

  private String buildJdbcUrl(
      DatabaseType databaseType, String host, int port, String databaseName) {
    return switch (databaseType) {
      case MYSQL ->
          "jdbc:mysql://"
              + host
              + ":"
              + port
              + "/"
              + databaseName
              + "?serverTimezone=UTC&rewriteBatchedStatements=true&useSSL=false&allowPublicKeyRetrieval=true";
      case POSTGRESQL -> "jdbc:postgresql://" + host + ":" + port + "/" + databaseName;
    };
  }

  private int defaultPort(DatabaseType databaseType) {
    return switch (databaseType) {
      case MYSQL -> 3306;
      case POSTGRESQL -> 5432;
    };
  }

  private String defaultUsername(DatabaseType databaseType) {
    return switch (databaseType) {
      case MYSQL -> "root";
      case POSTGRESQL -> "postgres";
    };
  }

  private String readRequired(Scanner scanner, String prompt) {
    while (true) {
      System.out.print(prompt + ": ");
      String value = scanner.nextLine();
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
      System.out.println("输入不能为空。");
    }
  }

  private String readOptional(Scanner scanner, String prompt) {
    System.out.print(prompt);
    String value = scanner.nextLine();
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private String readPassword(Scanner scanner) {
    String prompt = "请输入密码（可留空）: ";
    Console console = System.console();
    if (console != null) {
      char[] value = console.readPassword(prompt);
      return value == null ? "" : new String(value);
    }
    String value = readOptional(scanner, prompt);
    return value == null ? "" : value;
  }

  private record DataSourceSettings(
      DatabaseType databaseType,
      String url,
      String username,
      String password,
      String driverClassName) {}
}

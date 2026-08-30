package com.corwin.bootstrap.application.service;

import com.corwin.bootstrap.application.BootstrapTaskKey;
import com.corwin.bootstrap.application.BootstrapTaskReport;
import com.corwin.bootstrap.application.DatabaseType;
import com.corwin.bootstrap.interfaces.console.BootstrapConsolePrompt;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;

/**
 * @author Corwin 2026/4/28
 */
@Service
@RequiredArgsConstructor
public class BootstrapSchemaSyncService {

  private static final Pattern CREATE_TABLE_PATTERN =
      Pattern.compile(
          "(?is)create\\s+table\\s+(?:if\\s+not\\s+exists\\s+)?([`\"\\[]?[A-Za-z0-9_.$]+[`\"\\]]?)\\s*\\(");
  private static final Pattern CREATE_INDEX_PATTERN =
      Pattern.compile(
          "(?is)create\\s+(unique\\s+)?index\\s+([`\"\\[]?[A-Za-z0-9_.$]+[`\"\\]]?)\\s+on\\s+"
              + "([`\"\\[]?[A-Za-z0-9_.$]+[`\"\\]]?)\\s*\\(([^;]+?)\\)\\s*;");

  private final DataSource dataSource;
  private final BootstrapSqlTemplateService sqlTemplateService;
  private final BootstrapDatabaseTypeResolver databaseTypeResolver;
  private final BootstrapConsolePrompt consolePrompt;

  public BootstrapTaskReport run(boolean dryRun) {
    long startedAt = System.currentTimeMillis();
    List<BootstrapSqlTemplateService.ScriptResource> scripts =
        sqlTemplateService.listSchemaScripts();

    StringBuilder digestSource = new StringBuilder("schema\n");
    LinkedHashMap<String, TableDefinition> expectedTables = new LinkedHashMap<>();
    for (BootstrapSqlTemplateService.ScriptResource script : scripts) {
      digestSource.append(script.name()).append('\n').append(script.content()).append('\n');
      for (TableDefinition table : parseTables(script)) {
        expectedTables.put(table.lookupName(), table);
      }
    }

    if (expectedTables.isEmpty()) {
      return new BootstrapTaskReport(
          BootstrapTaskKey.SCHEMA_SYNC,
          dryRun,
          true,
          System.currentTimeMillis() - startedAt,
          "No table DDL found in system_schema.sql or business_schema.sql");
    }

    DatabaseType databaseType = databaseTypeResolver.resolveCurrentDatabaseType();

    try {
      SchemaInspectionResult inspectionResult =
          BootstrapJdbcTransactionSupport.execute(
              dataSource, connection -> inspectSchema(connection, expectedTables));
      SchemaDiffSummary summary = inspectionResult.summary();

      for (SchemaAction action : inspectionResult.actions()) {
        if (action.type() == SchemaActionType.CREATE) {
          printCreateAction(action.tableName(), dryRun);
          if (!dryRun) {
            BootstrapJdbcTransactionSupport.executeWithoutResult(
                dataSource,
                connection -> executeSql(connection, action.createStatement(), action.tableName()));
            summary.createdTables.add(action.tableName());
          }
          continue;
        }

        printMismatch(action.tableName(), action.mismatches());
        if (dryRun) {
          continue;
        }

        boolean recreate =
            consolePrompt.confirm("表 " + action.tableName() + " 结构不一致，是否删除旧表并重新创建？", false);
        if (!recreate) {
          summary.skippedRecreateTables.add(action.tableName());
          continue;
        }

        BootstrapJdbcTransactionSupport.executeWithoutResult(
            dataSource,
            connection -> {
              dropTable(connection, databaseType, action.actualTableName());
              executeSql(connection, action.createStatement(), action.tableName());
            });
        summary.recreatedTables.add(action.tableName());
      }

      return new BootstrapTaskReport(
          BootstrapTaskKey.SCHEMA_SYNC,
          dryRun,
          true,
          System.currentTimeMillis() - startedAt,
          summary.render(dryRun));
    } catch (Exception e) {
      throw new IllegalStateException("Schema bootstrap failed", e);
    }
  }

  private void printCreateAction(String tableName, boolean dryRun) {
    if (dryRun) {
      System.out.println("Table missing: " + tableName + " (dry-run, will create)");
      return;
    }
    System.out.println("Table missing: " + tableName + ", creating now.");
  }

  private void printMismatch(String tableName, List<String> mismatches) {
    System.out.println();
    System.out.println("Table schema mismatch: " + tableName);
    for (String mismatch : mismatches) {
      System.out.println("  - " + mismatch);
    }
  }

  private SchemaInspectionResult inspectSchema(
      Connection connection, Map<String, TableDefinition> expectedTables) throws Exception {
    DatabaseMetaData metaData = connection.getMetaData();
    SchemaDiffSummary summary = new SchemaDiffSummary();
    Map<String, String> actualTables = loadActualTables(connection, metaData);
    List<SchemaAction> actions = new ArrayList<>();

    for (TableDefinition expected : expectedTables.values()) {
      String actualTableName = actualTables.get(expected.lookupName());
      if (actualTableName == null) {
        summary.missingTables.add(expected.name());
        actions.add(
            new SchemaAction(
                SchemaActionType.CREATE,
                expected.name(),
                null,
                expected.createStatement(),
                List.of()));
        continue;
      }

      List<String> mismatches = compareTable(connection, metaData, expected, actualTableName);
      if (mismatches.isEmpty()) {
        continue;
      }

      summary.mismatchTables.add(expected.name());
      summary.mismatchMessages.add("Table " + expected.name() + " schema mismatch:");
      summary.mismatchMessages.addAll(mismatches.stream().map(item -> "  - " + item).toList());
      actions.add(
          new SchemaAction(
              SchemaActionType.RECREATE,
              expected.name(),
              actualTableName,
              expected.createStatement(),
              mismatches));
    }

    return new SchemaInspectionResult(summary, actions);
  }

  private List<TableDefinition> parseTables(BootstrapSqlTemplateService.ScriptResource script) {
    List<TableDefinition> result = new ArrayList<>();
    String content = script.content();
    Matcher matcher = CREATE_TABLE_PATTERN.matcher(content);
    List<MatchIndex> matches = new ArrayList<>();
    while (matcher.find()) {
      matches.add(new MatchIndex(matcher.start(), matcher.group(1), matcher.end() - 1));
    }
    for (int i = 0; i < matches.size(); i++) {
      MatchIndex current = matches.get(i);
      int blockEnd = i + 1 < matches.size() ? matches.get(i + 1).startIndex() : content.length();
      int closeParenIndex = findMatchingParenthesis(content, current.openParenIndex());
      String block = content.substring(current.startIndex(), blockEnd).trim();
      String inner = content.substring(current.openParenIndex() + 1, closeParenIndex);
      result.add(
          new TableDefinition(
              normalizeIdentifier(current.rawTableName()),
              parseColumns(inner),
              parseIndexes(block, current.rawTableName()),
              block));
    }
    return result;
  }

  private List<IndexDefinition> parseIndexes(String block, String rawTableName) {
    String targetLookupName = normalizeTableLookup(rawTableName);
    List<IndexDefinition> indexes = new ArrayList<>();
    Matcher matcher = CREATE_INDEX_PATTERN.matcher(block);
    while (matcher.find()) {
      String indexName = normalizeIdentifier(matcher.group(2));
      String tableName = matcher.group(3);
      if (!Objects.equals(targetLookupName, normalizeTableLookup(tableName))) {
        continue;
      }
      boolean unique = matcher.group(1) != null;
      List<String> columns = new ArrayList<>();
      for (String item : splitTopLevel(matcher.group(4))) {
        String column = normalizeIdentifier(item.trim());
        if (!column.isBlank()) {
          columns.add(column);
        }
      }
      indexes.add(new IndexDefinition(indexName, unique, columns));
    }
    return indexes;
  }

  private int findMatchingParenthesis(String content, int openParenIndex) {
    int depth = 0;
    boolean singleQuote = false;
    boolean doubleQuote = false;
    for (int i = openParenIndex; i < content.length(); i++) {
      char ch = content.charAt(i);
      if (ch == '\'' && !doubleQuote) {
        singleQuote = !singleQuote;
      } else if (ch == '"' && !singleQuote) {
        doubleQuote = !doubleQuote;
      }
      if (singleQuote || doubleQuote) {
        continue;
      }
      if (ch == '(') {
        depth++;
      } else if (ch == ')') {
        depth--;
        if (depth == 0) {
          return i;
        }
      }
    }
    throw new IllegalStateException("Unclosed CREATE TABLE statement");
  }

  private List<ColumnDefinition> parseColumns(String inner) {
    List<String> segments = splitTopLevel(inner);
    List<ColumnDefinition> columns = new ArrayList<>();
    int order = 1;
    for (String segment : segments) {
      String trimmed = segment.trim();
      if (trimmed.isEmpty() || isConstraintDefinition(trimmed)) {
        continue;
      }
      int firstSpace = findFirstWhitespace(trimmed);
      if (firstSpace < 0) {
        continue;
      }
      String rawName = trimmed.substring(0, firstSpace);
      String rest = trimmed.substring(firstSpace).trim();
      String lower = rest.toLowerCase(Locale.ROOT);
      boolean nullable = !lower.contains("not null");
      String normalizedType = normalizeExpectedType(extractTypeExpression(rest));
      columns.add(
          new ColumnDefinition(normalizeIdentifier(rawName), normalizedType, nullable, order++));
    }
    return columns;
  }

  private List<String> compareTable(
      Connection connection,
      DatabaseMetaData metaData,
      TableDefinition expected,
      String actualTableName)
      throws Exception {
    List<String> mismatches = new ArrayList<>();
    mismatches.addAll(compareColumns(connection, metaData, expected, actualTableName));
    mismatches.addAll(compareIndexes(connection, metaData, expected, actualTableName));
    return mismatches;
  }

  private List<String> compareColumns(
      Connection connection,
      DatabaseMetaData metaData,
      TableDefinition expected,
      String actualTableName)
      throws Exception {
    Map<String, ActualColumn> actualColumns =
        loadActualColumns(connection, metaData, actualTableName);
    List<String> mismatches = new ArrayList<>();
    LinkedHashMap<String, ColumnDefinition> expectedByName = new LinkedHashMap<>();
    for (ColumnDefinition column : expected.columns()) {
      expectedByName.put(column.lookupName(), column);
      ActualColumn actual = actualColumns.get(column.lookupName());
      if (actual == null) {
        mismatches.add(
            "missing column " + column.name() + ", expected type " + column.normalizedType());
        continue;
      }
      if (!isSameColumnType(column.normalizedType(), actual.normalizedType())) {
        mismatches.add(
            "column "
                + column.name()
                + " type mismatch, expected="
                + column.normalizedType()
                + ", actual="
                + actual.normalizedType());
      }
      if (column.nullable() != actual.nullable()) {
        mismatches.add(
            "column "
                + column.name()
                + " nullability mismatch, expected="
                + (column.nullable() ? "NULL" : "NOT NULL")
                + ", actual="
                + (actual.nullable() ? "NULL" : "NOT NULL"));
      }
      if (column.order() != actual.order()) {
        mismatches.add(
            "column "
                + column.name()
                + " order mismatch, expected="
                + column.order()
                + ", actual="
                + actual.order());
      }
    }
    for (ActualColumn actual : actualColumns.values()) {
      if (!expectedByName.containsKey(actual.lookupName())) {
        mismatches.add(
            "unexpected column " + actual.name() + ", actual type " + actual.normalizedType());
      }
    }
    return mismatches;
  }

  private boolean isSameColumnType(String expectedType, String actualType) {
    if (Objects.equals(expectedType, actualType)) {
      return true;
    }

    String normalizedExpected = stripLobLength(expectedType);
    String normalizedActual = stripLobLength(actualType);
    return Objects.equals(normalizedExpected, normalizedActual);
  }

  private String stripLobLength(String type) {
    if (type == null) {
      return null;
    }

    String normalized = Objects.requireNonNull(normalizeLookup(type)).replaceAll("\\s+", "");
    return normalized.replaceAll(
        "^(tinytext|text|mediumtext|longtext|tinyblob|blob|mediumblob|longblob)\\(\\d+\\)$", "$1");
  }

  private List<String> compareIndexes(
      Connection connection,
      DatabaseMetaData metaData,
      TableDefinition expected,
      String actualTableName)
      throws Exception {
    Map<String, ActualIndex> actualIndexes =
        loadActualIndexes(connection, metaData, actualTableName);
    List<String> mismatches = new ArrayList<>();
    for (IndexDefinition expectedIndex : expected.indexes()) {
      ActualIndex actualIndex = actualIndexes.get(expectedIndex.lookupName());
      if (actualIndex == null) {
        mismatches.add("missing index " + expectedIndex.name() + " on " + expectedIndex.columns());
        continue;
      }
      if (expectedIndex.unique() != actualIndex.unique()) {
        mismatches.add(
            "index "
                + expectedIndex.name()
                + " uniqueness mismatch, expected="
                + expectedIndex.unique()
                + ", actual="
                + actualIndex.unique());
      }
      if (!Objects.equals(expectedIndex.lookupColumns(), actualIndex.lookupColumns())) {
        mismatches.add(
            "index "
                + expectedIndex.name()
                + " columns mismatch, expected="
                + expectedIndex.columns()
                + ", actual="
                + actualIndex.columns());
      }
    }
    return mismatches;
  }

  private Map<String, String> loadActualTables(Connection connection, DatabaseMetaData metaData)
      throws Exception {
    String catalog = connection.getCatalog();
    String schema = connection.getSchema();
    LinkedHashMap<String, String> tables = new LinkedHashMap<>();
    try (ResultSet rs = metaData.getTables(catalog, schema, "%", new String[] {"TABLE"})) {
      while (rs.next()) {
        String name = rs.getString("TABLE_NAME");
        tables.put(normalizeTableLookup(name), name);
      }
    }
    if (!tables.isEmpty()) {
      return tables;
    }
    try (ResultSet rs = metaData.getTables(null, null, "%", new String[] {"TABLE"})) {
      while (rs.next()) {
        String name = rs.getString("TABLE_NAME");
        tables.put(normalizeTableLookup(name), name);
      }
    }
    return tables;
  }

  private Map<String, ActualColumn> loadActualColumns(
      Connection connection, DatabaseMetaData metaData, String tableName) throws Exception {
    String catalog = connection.getCatalog();
    String schema = connection.getSchema();
    LinkedHashMap<String, ActualColumn> result = new LinkedHashMap<>();
    try (ResultSet rs = metaData.getColumns(catalog, schema, tableName, "%")) {
      while (rs.next()) {
        String name = rs.getString("COLUMN_NAME");
        result.put(
            normalizeLookup(name),
            new ActualColumn(
                name,
                normalizeActualType(
                    rs.getInt("DATA_TYPE"),
                    rs.getString("TYPE_NAME"),
                    rs.getInt("COLUMN_SIZE"),
                    rs.getInt("DECIMAL_DIGITS")),
                rs.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls,
                rs.getInt("ORDINAL_POSITION")));
      }
    }
    if (!result.isEmpty()) {
      return result;
    }
    try (ResultSet rs = metaData.getColumns(null, null, tableName, "%")) {
      while (rs.next()) {
        String name = rs.getString("COLUMN_NAME");
        result.put(
            normalizeLookup(name),
            new ActualColumn(
                name,
                normalizeActualType(
                    rs.getInt("DATA_TYPE"),
                    rs.getString("TYPE_NAME"),
                    rs.getInt("COLUMN_SIZE"),
                    rs.getInt("DECIMAL_DIGITS")),
                rs.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls,
                rs.getInt("ORDINAL_POSITION")));
      }
    }
    return result;
  }

  private Map<String, ActualIndex> loadActualIndexes(
      Connection connection, DatabaseMetaData metaData, String tableName) throws Exception {
    String catalog = connection.getCatalog();
    String schema = connection.getSchema();
    LinkedHashMap<String, ActualIndexBuilder> builders = new LinkedHashMap<>();
    loadActualIndexes(metaData, catalog, schema, tableName, builders);
    if (builders.isEmpty()) {
      loadActualIndexes(metaData, null, null, tableName, builders);
    }

    LinkedHashMap<String, ActualIndex> result = new LinkedHashMap<>();
    for (ActualIndexBuilder builder : builders.values()) {
      result.put(builder.lookupName(), builder.build());
    }
    return result;
  }

  private void loadActualIndexes(
      DatabaseMetaData metaData,
      String catalog,
      String schema,
      String tableName,
      Map<String, ActualIndexBuilder> builders)
      throws Exception {
    try (ResultSet rs = metaData.getIndexInfo(catalog, schema, tableName, false, false)) {
      while (rs.next()) {
        short type = rs.getShort("TYPE");
        String indexName = rs.getString("INDEX_NAME");
        String columnName = rs.getString("COLUMN_NAME");
        if (type == DatabaseMetaData.tableIndexStatistic
            || indexName == null
            || columnName == null) {
          continue;
        }
        boolean unique = !rs.getBoolean("NON_UNIQUE");
        ActualIndexBuilder builder =
            builders.computeIfAbsent(
                normalizeLookup(indexName), key -> new ActualIndexBuilder(indexName, unique));
        builder.addColumn(rs.getShort("ORDINAL_POSITION"), columnName);
      }
    }
  }

  private void dropTable(Connection connection, DatabaseType databaseType, String tableName)
      throws SQLException {
    String sql =
        "drop table if exists " + BootstrapJdbcUrlSupport.quoteIdentifier(databaseType, tableName);
    try (var statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  private void executeSql(Connection connection, String sql, String scriptName) {
    ScriptUtils.executeSqlScript(
        connection, new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8), scriptName));
  }

  private List<String> splitTopLevel(String content) {
    List<String> result = new ArrayList<>();
    StringBuilder builder = new StringBuilder();
    int depth = 0;
    boolean singleQuote = false;
    boolean doubleQuote = false;
    for (int i = 0; i < content.length(); i++) {
      char ch = content.charAt(i);
      if (ch == '\'' && !doubleQuote) {
        singleQuote = !singleQuote;
      } else if (ch == '"' && !singleQuote) {
        doubleQuote = !doubleQuote;
      }
      if (!singleQuote && !doubleQuote) {
        if (ch == '(') {
          depth++;
        } else if (ch == ')') {
          depth--;
        } else if (ch == ',' && depth == 0) {
          result.add(builder.toString());
          builder.setLength(0);
          continue;
        }
      }
      builder.append(ch);
    }
    if (!builder.isEmpty()) {
      result.add(builder.toString());
    }
    return result;
  }

  private boolean isConstraintDefinition(String definition) {
    String normalized = definition.trim().toLowerCase(Locale.ROOT);
    return normalized.startsWith("primary key")
        || normalized.startsWith("unique")
        || normalized.startsWith("constraint")
        || normalized.startsWith("index")
        || normalized.startsWith("key")
        || normalized.startsWith("foreign key");
  }

  private int findFirstWhitespace(String value) {
    for (int i = 0; i < value.length(); i++) {
      if (Character.isWhitespace(value.charAt(i))) {
        return i;
      }
    }
    return -1;
  }

  private String extractTypeExpression(String definition) {
    String[] tokens = definition.split("\\s+");
    List<String> collected = new ArrayList<>();
    for (String token : tokens) {
      String lower = token.toLowerCase(Locale.ROOT);
      if (lower.equals("not")
          || lower.equals("null")
          || lower.equals("default")
          || lower.equals("comment")
          || lower.equals("constraint")
          || lower.equals("primary")
          || lower.equals("unique")
          || lower.equals("references")
          || lower.equals("check")
          || lower.equals("auto_increment")
          || lower.equals("identity")
          || lower.equals("generated")
          || lower.equals("always")
          || lower.equals("as")) {
        break;
      }
      collected.add(token);
    }
    return String.join(" ", collected);
  }

  private String normalizeExpectedType(String typeExpression) {
    String normalized =
        Objects.requireNonNull(normalizeLookup(typeExpression)).replaceAll("\\s+", "");
    return normalized.replace("unsigned", "");
  }

  private String normalizeActualType(int jdbcType, String typeName, int size, int scale) {
    return switch (jdbcType) {
      case Types.VARCHAR,
          Types.NVARCHAR,
          Types.CHAR,
          Types.NCHAR,
          Types.LONGVARCHAR,
          Types.LONGNVARCHAR ->
          normalizeLookup(typeName) + "(" + size + ")";
      case Types.DECIMAL, Types.NUMERIC ->
          normalizeLookup(typeName) + "(" + size + "," + scale + ")";
      case Types.TIMESTAMP,
          Types.TIMESTAMP_WITH_TIMEZONE,
          Types.DATE,
          Types.TIME,
          Types.TIME_WITH_TIMEZONE,
          Types.BIGINT,
          Types.INTEGER,
          Types.SMALLINT,
          Types.TINYINT,
          Types.BOOLEAN,
          Types.BIT,
          Types.CLOB,
          Types.BLOB,
          Types.BINARY,
          Types.VARBINARY,
          Types.LONGVARBINARY,
          Types.DOUBLE,
          Types.FLOAT,
          Types.REAL ->
          normalizeLookup(typeName);
      default -> normalizeLookup(typeName);
    };
  }

  private static String normalizeIdentifier(String value) {
    return value == null
        ? null
        : value.replace("`", "").replace("\"", "").replace("[", "").replace("]", "").trim();
  }

  private static String normalizeLookup(String value) {
    String normalized = normalizeIdentifier(value);
    return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
  }

  private static String normalizeTableLookup(String value) {
    String normalized = normalizeIdentifier(value);
    if (normalized == null || normalized.isBlank()) {
      return normalized;
    }
    int lastDot = normalized.lastIndexOf('.');
    String tableName = lastDot >= 0 ? normalized.substring(lastDot + 1) : normalized;
    return tableName.toLowerCase(Locale.ROOT);
  }

  private record MatchIndex(int startIndex, String rawTableName, int openParenIndex) {}

  private record TableDefinition(
      String name,
      List<ColumnDefinition> columns,
      List<IndexDefinition> indexes,
      String createStatement) {
    private String lookupName() {
      return normalizeTableLookup(name);
    }
  }

  private record ColumnDefinition(String name, String normalizedType, boolean nullable, int order) {
    private String lookupName() {
      return normalizeLookup(name);
    }
  }

  private record IndexDefinition(String name, boolean unique, List<String> columns) {
    private String lookupName() {
      return normalizeLookup(name);
    }

    private List<String> lookupColumns() {
      return columns.stream().map(BootstrapSchemaSyncService::normalizeLookup).toList();
    }
  }

  private record ActualColumn(String name, String normalizedType, boolean nullable, int order) {
    private String lookupName() {
      return normalizeLookup(name);
    }
  }

  private record ActualIndex(String name, boolean unique, List<String> columns) {
    private List<String> lookupColumns() {
      return columns.stream().map(BootstrapSchemaSyncService::normalizeLookup).toList();
    }
  }

  private static final class ActualIndexBuilder {
    private final String name;
    private final boolean unique;
    private final Map<Short, String> orderedColumns = new LinkedHashMap<>();

    private ActualIndexBuilder(String name, boolean unique) {
      this.name = name;
      this.unique = unique;
    }

    private String lookupName() {
      return normalizeLookup(name);
    }

    private void addColumn(short order, String columnName) {
      orderedColumns.put(order, columnName);
    }

    private ActualIndex build() {
      List<String> columns =
          orderedColumns.entrySet().stream()
              .sorted(Map.Entry.comparingByKey())
              .map(Map.Entry::getValue)
              .toList();
      return new ActualIndex(name, unique, columns);
    }
  }

  private static final class SchemaDiffSummary {
    private final List<String> missingTables = new ArrayList<>();
    private final List<String> mismatchTables = new ArrayList<>();
    private final List<String> mismatchMessages = new ArrayList<>();
    private final List<String> createdTables = new ArrayList<>();
    private final List<String> recreatedTables = new ArrayList<>();
    private final List<String> skippedRecreateTables = new ArrayList<>();

    private String render(boolean dryRun) {
      List<String> parts = new ArrayList<>();
      parts.add("missingTables=" + missingTables.size());
      parts.add("mismatchTables=" + mismatchTables.size());
      if (!dryRun) {
        parts.add("createdTables=" + createdTables.size());
        parts.add("recreatedTables=" + recreatedTables.size());
        parts.add("skippedRecreateTables=" + skippedRecreateTables.size());
      }

      List<String> details = new ArrayList<>();
      if (!missingTables.isEmpty()) {
        details.add("missing=" + missingTables);
      }
      if (!mismatchMessages.isEmpty()) {
        details.addAll(mismatchMessages);
      }
      if (!skippedRecreateTables.isEmpty()) {
        details.add("skippedRecreate=" + skippedRecreateTables);
      }
      return String.join("; ", parts)
          + (details.isEmpty() ? "" : " | " + String.join(" | ", details));
    }
  }

  private record SchemaInspectionResult(SchemaDiffSummary summary, List<SchemaAction> actions) {}

  private record SchemaAction(
      SchemaActionType type,
      String tableName,
      String actualTableName,
      String createStatement,
      List<String> mismatches) {}

  private enum SchemaActionType {
    CREATE,
    RECREATE
  }
}

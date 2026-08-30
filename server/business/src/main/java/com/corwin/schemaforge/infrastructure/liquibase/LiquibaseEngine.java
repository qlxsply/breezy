package com.corwin.schemaforge.infrastructure.liquibase;

import com.corwin.framework.config.runtime.Configs;
import com.corwin.schemaforge.config.SchemaForgeConfigSpecs;
import com.corwin.schemaforge.config.SchemaForgeConfigSpecs.DdlPolicy;
import com.corwin.schemaforge.config.SchemaForgeConfigSpecs.QualifierMode;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;
import liquibase.change.Change;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.diff.DiffGeneratorFactory;
import liquibase.diff.DiffResult;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.ObjectChangeFilter;
import liquibase.diff.output.StandardObjectChangeFilter;
import liquibase.diff.output.changelog.DiffToChangeLog;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.parser.SnapshotParser;
import liquibase.parser.SnapshotParserFactory;
import liquibase.resource.Resource;
import liquibase.resource.ResourceAccessor;
import liquibase.serializer.SnapshotSerializer;
import liquibase.serializer.SnapshotSerializerFactory;
import liquibase.serializer.core.xml.XMLChangeLogSerializer;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.EmptyDatabaseSnapshot;
import liquibase.snapshot.SnapshotControl;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.springframework.stereotype.Component;

/**
 * SchemaForge Liquibase 寮曟搸锛岃礋璐ｅ揩鐓с€丏DL銆丏iff 鐨勫唴瀛樻祦鐢熸垚銆? *
 *
 * @author Corwin 2026/2/24
 */
@Slf4j
@Component
public class LiquibaseEngine {

  public SnapshotResult generateSnapshot(Connection connection) {
    return generateSnapshot(connection, null, null);
  }

  public SnapshotResult generateSnapshot(
      Connection connection, Set<String> selectedTableNames, Set<String> selectedViewNames) {
    try {
      Database database = toDatabase(connection);
      SnapshotControl snapshotControl =
          createSnapshotControl(database, selectedTableNames, selectedViewNames);
      DatabaseSnapshot snapshot =
          SnapshotGeneratorFactory.getInstance()
              .createSnapshot(database.getDefaultSchema(), database, snapshotControl);
      byte[] content = serializeSnapshot(snapshot);

      return new SnapshotResult(
          content, database.getShortName(), databaseVersion(database), schemaName(database));
    } catch (Exception e) {
      throw new RuntimeException("Generate schema snapshot failed: " + e.getMessage(), e);
    }
  }

  public DdlResult generateDdl(Connection connection) {
    return generateDdl(connection, null, null);
  }

  public DdlResult generateDdl(
      Connection connection, Set<String> selectedTableNames, Set<String> selectedViewNames) {
    try {
      SnapshotContext context =
          buildSnapshotContext(connection, selectedTableNames, selectedViewNames);
      DdlPolicy policy = ddlPolicy();
      String sql =
          renderSql(context.diffResult(), context.database(), new DiffOutputControl(), policy);
      return new DdlResult(
          sql.getBytes(StandardCharsets.UTF_8),
          context.database().getShortName(),
          databaseVersion(context.database()),
          schemaName(context.database()));
    } catch (Exception e) {
      throw new RuntimeException("Generate schema ddl failed: " + e.getMessage(), e);
    }
  }

  public DdlResult generateDdlFromSnapshots(
      byte[] sourceSnapshotContent,
      byte[] targetSnapshotContent,
      String sourceQualifierHint,
      String targetQualifierHint) {
    try {
      DatabaseSnapshot targetSnapshot =
          parseSnapshot(targetSnapshotContent, "target.snapshot.yaml");
      DatabaseSnapshot sourceSnapshot = buildSourceSnapshot(sourceSnapshotContent, targetSnapshot);

      DiffResult diffResult =
          DiffGeneratorFactory.getInstance()
              .compare(targetSnapshot, sourceSnapshot, new CompareControl());
      Database targetDatabase = targetSnapshot.getDatabase();
      Database sourceDatabase = sourceSnapshot.getDatabase();
      String targetQualifier = resolveDatabaseQualifier(targetDatabase, targetQualifierHint);
      String sourceQualifier = resolveDatabaseQualifier(sourceDatabase, sourceQualifierHint);
      DdlPolicy policy = ddlPolicy();
      QualifierMode qualifierMode = policy.qualifierMode();
      DiffOutputControl outputControl =
          buildOutputControlForSnapshots(qualifierMode, sourceQualifier, targetQualifier);
      String sql = renderSql(diffResult, sourceDatabase, outputControl, policy);
      if (qualifierMode == QualifierMode.ALWAYS_SOURCE) {
        sql = alignSqlQualifierToSource(sql, sourceQualifier, targetQualifier);
      }
      return new DdlResult(
          sql.getBytes(StandardCharsets.UTF_8),
          sourceDatabase.getShortName(),
          databaseVersion(sourceDatabase),
          schemaName(sourceDatabase));
    } catch (Exception e) {
      throw new RuntimeException("Generate ddl from snapshots failed: " + e.getMessage(), e);
    }
  }

  public CompareResult compare(Connection referenceConnection, Connection targetConnection) {
    try {
      Database referenceDatabase = toDatabase(referenceConnection);
      Database targetDatabase = toDatabase(targetConnection);
      DiffResult diffResult =
          DiffGeneratorFactory.getInstance()
              .compare(targetDatabase, referenceDatabase, new CompareControl());

      String xml = renderChangeLogXml(diffResult);
      DdlPolicy policy = ddlPolicy();
      String sql = renderSql(diffResult, referenceDatabase, new DiffOutputControl(), policy);
      return new CompareResult(
          diffResult.getMissingObjects().size(),
          diffResult.getUnexpectedObjects().size(),
          diffResult.getChangedObjects().size(),
          xml,
          sql);
    } catch (Exception e) {
      throw new RuntimeException("Compare schema diff failed: " + e.getMessage(), e);
    }
  }

  private SnapshotContext buildSnapshotContext(Connection connection) throws Exception {
    return buildSnapshotContext(connection, null, null);
  }

  private SnapshotContext buildSnapshotContext(
      Connection connection, Set<String> selectedTableNames, Set<String> selectedViewNames)
      throws Exception {
    Database database = toDatabase(connection);
    SnapshotControl snapshotControl =
        createSnapshotControl(database, selectedTableNames, selectedViewNames);
    DatabaseSnapshot snapshot =
        SnapshotGeneratorFactory.getInstance()
            .createSnapshot(database.getDefaultSchema(), database, snapshotControl);
    DatabaseSnapshot emptySnapshot = new EmptyDatabaseSnapshot(database, snapshotControl);
    DiffResult diffResult =
        DiffGeneratorFactory.getInstance().compare(snapshot, emptySnapshot, new CompareControl());
    return new SnapshotContext(database, diffResult);
  }

  private Database toDatabase(Connection connection) throws DatabaseException {
    Objects.requireNonNull(connection, "connection required");
    Database database =
        DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(new JdbcConnection(connection));
    applyDefaultSchema(connection, database);
    return database;
  }

  private SnapshotControl createSnapshotControl(
      Database database, Set<String> selectedTableNames, Set<String> selectedViewNames) {
    ObjectChangeFilter objectChangeFilter =
        buildObjectChangeFilter(selectedTableNames, selectedViewNames);
    if (objectChangeFilter == null) {
      return new SnapshotControl(database);
    }
    return new SnapshotControl(database, objectChangeFilter);
  }

  private ObjectChangeFilter buildObjectChangeFilter(
      Set<String> selectedTableNames, Set<String> selectedViewNames) {
    String tablePattern = toObjectNamePattern(selectedTableNames);
    String viewPattern = toObjectNamePattern(selectedViewNames);
    if (tablePattern == null && viewPattern == null) {
      return null;
    }

    StringBuilder filterBuilder = new StringBuilder();
    if (tablePattern != null) {
      filterBuilder.append("Table:(?i)").append(tablePattern);
    }
    if (viewPattern != null) {
      if (!filterBuilder.isEmpty()) {
        filterBuilder.append(',');
      }
      filterBuilder.append("View:(?i)").append(viewPattern);
    }
    return new StandardObjectChangeFilter(
        StandardObjectChangeFilter.FilterType.INCLUDE, filterBuilder.toString());
  }

  private String toObjectNamePattern(Set<String> names) {
    if (names == null || names.isEmpty()) {
      return null;
    }
    String merged =
        names.stream()
            .map(this::normalizeObjectName)
            .filter(item -> item != null && !item.isEmpty())
            .map(Pattern::quote)
            .collect(Collectors.joining("|"));
    if (merged.isEmpty()) {
      return null;
    }
    return "^(" + merged + ")$";
  }

  private String normalizeObjectName(String objectName) {
    if (objectName == null) {
      return null;
    }
    return objectName.trim();
  }

  private void applyDefaultSchema(Connection connection, Database database) {
    try {
      String schema = connection.getSchema();
      if (schema != null && !schema.isBlank()) {
        database.setDefaultSchemaName(schema);
      }
    } catch (SQLException | DatabaseException ignored) {
    }

    try {
      String catalog = connection.getCatalog();
      if (catalog != null && !catalog.isBlank()) {
        database.setDefaultCatalogName(catalog);
      }
    } catch (SQLException | DatabaseException ignored) {
    }
  }

  private byte[] serializeSnapshot(DatabaseSnapshot snapshot) throws IOException {
    SnapshotSerializer serializer = SnapshotSerializerFactory.getInstance().getSerializer("yaml");
    if (serializer == null) {
      throw new IllegalStateException("No snapshot serializer found for yaml");
    }

    String content = serializer.serialize(snapshot, true);
    return content.getBytes(StandardCharsets.UTF_8);
  }

  private DatabaseSnapshot parseSnapshot(byte[] content, String path) throws LiquibaseException {
    if (content == null || content.length == 0) {
      throw new IllegalArgumentException("snapshot content required");
    }

    MemoryResourceAccessor resourceAccessor = new MemoryResourceAccessor(Map.of(path, content));
    SnapshotParser parser = SnapshotParserFactory.getInstance().getParser(path, resourceAccessor);
    return parser.parse(path, resourceAccessor);
  }

  private DatabaseSnapshot buildSourceSnapshot(
      byte[] sourceSnapshotContent, DatabaseSnapshot targetSnapshot)
      throws DatabaseException, LiquibaseException {
    if (sourceSnapshotContent == null || sourceSnapshotContent.length == 0) {
      SnapshotControl snapshotControl =
          targetSnapshot.getSnapshotControl() == null
              ? new SnapshotControl(targetSnapshot.getDatabase())
              : targetSnapshot.getSnapshotControl();
      return new EmptyDatabaseSnapshot(targetSnapshot.getDatabase(), snapshotControl);
    }
    return parseSnapshot(sourceSnapshotContent, "source.snapshot.yaml");
  }

  private String renderChangeLogXml(DiffResult diffResult)
      throws ParserConfigurationException, IOException, DatabaseException {
    DiffToChangeLog diffToChangeLog = new DiffToChangeLog(diffResult, new DiffOutputControl());
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (PrintStream printStream = new PrintStream(output, true, StandardCharsets.UTF_8)) {
      diffToChangeLog.print(printStream, new XMLChangeLogSerializer());
    }
    return output.toString(StandardCharsets.UTF_8);
  }

  private String renderSql(
      DiffResult diffResult,
      Database targetDatabase,
      DiffOutputControl outputControl,
      DdlPolicy policy) {
    DiffToChangeLog diffToChangeLog = new DiffToChangeLog(diffResult, outputControl);
    StringBuilder builder = new StringBuilder();
    for (ChangeSet changeSet : diffToChangeLog.generateChangeSets()) {
      for (Change change : changeSet.getChanges()) {
        Sql[] sqlArray = SqlGeneratorFactory.getInstance().generateSql(change, targetDatabase);
        appendSql(builder, sqlArray, policy.formatEnabled());
      }
    }
    return builder.toString().trim();
  }

  private void appendSql(StringBuilder builder, Sql[] sqlArray, boolean formatEnabled) {
    if (sqlArray == null || sqlArray.length == 0) {
      return;
    }

    for (Sql sql : sqlArray) {
      if (sql == null) {
        continue;
      }
      String sqlText = sql.toSql();
      if (sqlText == null || sqlText.isBlank()) {
        continue;
      }
      sqlText = sqlText.trim();
      sqlText = formatSqlIfEnabled(sqlText, formatEnabled);
      builder.append(sqlText);
      String delimiter = sql.getEndDelimiter();
      if (delimiter == null || delimiter.isBlank()) {
        if (!sqlText.endsWith(";")) {
          builder.append(';');
        }
      } else if (!sqlText.endsWith(delimiter)) {
        builder.append(delimiter);
      }
      builder.append(System.lineSeparator()).append(System.lineSeparator());
    }
  }

  private String formatSqlIfEnabled(String sqlText, boolean formatEnabled) {
    if (!formatEnabled) {
      return sqlText;
    }
    try {
      String formatted = FormatStyle.DDL.getFormatter().format(sqlText);
      if (formatted == null || formatted.isBlank()) {
        return sqlText;
      }
      return formatted.trim();
    } catch (Exception e) {
      log.warn("SchemaForge DDL 鏍煎紡鍖栧け璐ワ紝鍥為€€鍘熷 SQL", e);
      return sqlText;
    }
  }

  private DiffOutputControl buildOutputControlForSnapshots(
      QualifierMode qualifierMode, String sourceQualifier, String targetQualifier) {
    DiffOutputControl outputControl = new DiffOutputControl();
    switch (qualifierMode) {
      case NEVER -> {
        outputControl.setIncludeCatalog(false);
        outputControl.setIncludeSchema(false);
      }
      case AUTO -> {
        boolean sameQualifier =
            Objects.equals(
                normalizeQualifier(sourceQualifier), normalizeQualifier(targetQualifier));
        outputControl.setIncludeCatalog(sameQualifier);
        outputControl.setIncludeSchema(sameQualifier);
      }
      case ALWAYS_SOURCE -> {
        outputControl.setIncludeCatalog(true);
        outputControl.setIncludeSchema(true);
      }
    }
    return outputControl;
  }

  private String alignSqlQualifierToSource(
      String sql, String sourceQualifier, String targetQualifier) {
    if (sql == null || sql.isBlank()) {
      return sql;
    }
    String normalizedTarget = normalizeQualifier(targetQualifier);
    if (normalizedTarget == null) {
      return sql;
    }

    String normalizedSource = normalizeQualifier(sourceQualifier);
    if (Objects.equals(normalizedSource, normalizedTarget)) {
      return sql;
    }

    String result = sql;
    if (normalizedSource == null) {
      result = result.replace("`" + targetQualifier + "`.", "");
      result = result.replace("\"" + targetQualifier + "\".", "");
      Pattern stripPattern = Pattern.compile("(?i)\\b" + Pattern.quote(targetQualifier) + "\\.");
      return stripPattern.matcher(result).replaceAll("");
    }

    result = result.replace("`" + targetQualifier + "`.", "`" + sourceQualifier + "`.");
    result = result.replace("\"" + targetQualifier + "\".", "\"" + sourceQualifier + "\".");

    Pattern plainQualifierPattern =
        Pattern.compile("(?i)\\b" + Pattern.quote(targetQualifier) + "\\.");
    result =
        plainQualifierPattern
            .matcher(result)
            .replaceAll(Matcher.quoteReplacement(sourceQualifier + "."));
    return result;
  }

  private String resolveDatabaseQualifier(Database database, String qualifierHint) {
    if (database == null) {
      return normalizeQualifier(qualifierHint);
    }
    String catalog = normalizeQualifier(database.getDefaultCatalogName());
    if (catalog != null) {
      return catalog;
    }
    String schema = normalizeQualifier(database.getDefaultSchemaName());
    if (schema != null) {
      return schema;
    }
    return normalizeQualifier(qualifierHint);
  }

  private String normalizeQualifier(String qualifier) {
    if (qualifier == null || qualifier.isBlank()) {
      return null;
    }
    return qualifier.trim();
  }

  private DdlPolicy ddlPolicy() {
    return Configs.get(SchemaForgeConfigSpecs.DDL_POLICY);
  }

  private String databaseVersion(Database database) {
    try {
      return database.getDatabaseProductVersion();
    } catch (DatabaseException e) {
      return null;
    }
  }

  private String schemaName(Database database) {
    String schemaName = database.getDefaultSchemaName();
    if (schemaName != null && !schemaName.isBlank()) {
      return schemaName;
    }
    String catalogName = database.getDefaultCatalogName();
    if (catalogName != null && !catalogName.isBlank()) {
      return catalogName;
    }
    return null;
  }

  public record SnapshotResult(
      byte[] content, String dbType, String dbVersion, String schemaName) {}

  public record DdlResult(byte[] content, String dbType, String dbVersion, String schemaName) {}

  public record CompareResult(
      int addedCount, int removedCount, int changedCount, String changeLogXml, String changeSql) {}

  private record SnapshotContext(Database database, DiffResult diffResult) {}

  private static final class MemoryResourceAccessor implements ResourceAccessor {

    private final Map<String, MemoryResource> resources;

    private MemoryResourceAccessor(Map<String, byte[]> contents) {
      this.resources =
          contents.entrySet().stream()
              .collect(
                  Collectors.toMap(
                      Map.Entry::getKey,
                      entry -> new MemoryResource(entry.getKey(), entry.getValue())));
    }

    @Override
    public List<Resource> search(String path, boolean recursive) {
      Resource resource = resources.get(path);
      if (resource == null) {
        return List.of();
      }
      return List.of(resource);
    }

    @Override
    public List<Resource> getAll(String path) {
      Resource resource = resources.get(path);
      if (resource == null) {
        return List.of();
      }
      return List.of(resource);
    }

    @Override
    public List<String> describeLocations() {
      return List.of("memory");
    }

    @Override
    public void close() {}
  }

  private static final class MemoryResource implements Resource {

    private final String path;
    private final byte[] content;

    private MemoryResource(String path, byte[] content) {
      this.path = path;
      this.content = content;
    }

    @Override
    public String getPath() {
      return path;
    }

    @Override
    public ByteArrayInputStream openInputStream() {
      return new ByteArrayInputStream(content);
    }

    @Override
    public boolean isWritable() {
      return false;
    }

    @Override
    public boolean exists() {
      return true;
    }

    @Override
    public Resource resolve(String other) {
      return this;
    }

    @Override
    public Resource resolveSibling(String other) {
      return this;
    }

    @Override
    public OutputStream openOutputStream(liquibase.resource.OpenOptions openOptions)
        throws IOException {
      throw new IOException("MemoryResource is read-only");
    }

    @Override
    public URI getUri() {
      return URI.create("memory:///" + path);
    }
  }
}

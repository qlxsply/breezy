package com.corwin.datasource.application.service;

import com.corwin.datasource.application.command.UpsertConnectionCommand;
import com.corwin.datasource.application.port.AppDataSourceProvider;
import com.corwin.datasource.application.port.MetadataExtractResult;
import com.corwin.datasource.application.port.MetadataExtractor;
import com.corwin.datasource.application.port.SecretCodec;
import com.corwin.datasource.application.view.DatabaseSchemaBasicInfoView;
import com.corwin.datasource.application.view.DatabaseSourceConnectionInfoView;
import com.corwin.datasource.application.view.DatabaseSourceSimpleView;
import com.corwin.datasource.application.view.TestConnectionView;
import com.corwin.datasource.domain.error.DatabaseSourceError;
import com.corwin.datasource.domain.model.*;
import com.corwin.datasource.domain.repo.*;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.util.HighDate;
import com.corwin.framework.util.StrUtil;
import com.corwin.framework.web.sort.PageSpecSorts;
import java.time.Instant;
import java.util.*;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 数据源与数据库管理服务
 *
 * @author Corwin 2026/1/11
 */
@Service
@AllArgsConstructor
public class DatabaseSourceAdminService {

  private static final Set<DatabaseType> USER_SUPPORTED_DB_TYPES =
      EnumSet.of(DatabaseType.MYSQL, DatabaseType.POSTGRESQL, DatabaseType.ORACLE);

  private final DatabaseSourceRepository databaseSourceRepo;
  private final DatabaseSchemaRepository databaseSchemaRepo;
  private final DatabaseTableRepository tableRepo;
  private final DatabaseColumnRepository columnRepo;
  private final SecretCodec secretCodec;
  private final AppDataSourceProvider appDataSourceProvider;
  private final MetadataExtractor metadataExtractor;

  // --- 数据源管理 ---

  @Transactional
  public Long upsertDatabaseSource(UpsertConnectionCommand cmd) {
    UpsertConnectionCommand normalized = normalizeCommand(cmd);
    DatabaseSource databaseSource;
    if (normalized.id() == null) {
      validateUserConnectionCommand(normalized);
      String passwordEnc =
          secretCodec.encode(
              Objects.requireNonNull(normalized.passwordRaw(), "passwordRaw required"));
      databaseSource =
          DatabaseSource.createUser(
              normalized.name(),
              normalized.dbType(),
              normalized.jdbcUrl(),
              normalized.username(),
              passwordEnc,
              normalized.remarkCustom(),
              normalized.authMode(),
              normalized.connectMode(),
              normalized.host(),
              normalized.port(),
              normalized.databaseName(),
              normalized.serviceName(),
              normalized.sid(),
              normalized.driverClassName(),
              normalized.extraParams());
    } else {
      databaseSource =
          databaseSourceRepo
              .findById(normalized.id())
              .orElseThrow(
                  () -> new IllegalArgumentException("DataSource not found: " + normalized.id()));
      if (databaseSource.isAppSource()) {
        databaseSource.updateName(normalized.name());
      } else {
        validateUserConnectionCommand(normalized);
        String passwordEnc = resolvePasswordEnc(normalized, databaseSource);
        databaseSource.updateUserConfig(
            normalized.name(),
            normalized.dbType(),
            normalized.jdbcUrl(),
            normalized.username(),
            passwordEnc,
            normalized.remarkCustom(),
            normalized.authMode(),
            normalized.connectMode(),
            normalized.host(),
            normalized.port(),
            normalized.databaseName(),
            normalized.serviceName(),
            normalized.sid(),
            normalized.driverClassName(),
            normalized.extraParams());
      }
    }

    databaseSourceRepo.save(databaseSource);
    return databaseSource.getId();
  }

  @Transactional
  public void testDatabaseSource(Long id) {
    DatabaseSource databaseSource =
        databaseSourceRepo
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("DataSource not found: " + id));
    testDatabaseSourceInternal(databaseSource);
  }

  @Transactional
  public void testAllDatabaseSources() {
    List<DatabaseSource> sources = databaseSourceRepo.findAll();
    for (DatabaseSource source : sources) {
      testDatabaseSourceInternal(source);
    }
  }

  private void testDatabaseSourceInternal(DatabaseSource source) {
    Instant now = HighDate.mockInstant();
    if (source.isAppSource()) {
      source.markTestOk(now);
      databaseSourceRepo.save(source);
      return;
    }
    try {
      metadataExtractor.testConnection(source);
      source.markTestOk(now);
    } catch (RuntimeException ex) {
      source.markTestFailed(now, ex.getMessage());
    }
    databaseSourceRepo.save(source);
  }

  public TestConnectionView testDatabaseSourceConfig(UpsertConnectionCommand cmd) {
    UpsertConnectionCommand normalized = normalizeCommand(cmd);
    if (normalized.id() != null) {
      DatabaseSource existing =
          databaseSourceRepo
              .findById(normalized.id())
              .orElseThrow(
                  () -> new IllegalArgumentException("DataSource not found: " + normalized.id()));
      if (existing.isAppSource()) {
        return TestConnectionView.ok();
      }
    }
    validateUserConnectionCommand(normalized);

    String passwordEnc;
    if (normalized.passwordRaw() != null && !normalized.passwordRaw().isBlank()) {
      passwordEnc = secretCodec.encode(normalized.passwordRaw());
    } else if (normalized.id() != null) {
      DatabaseSource existing =
          databaseSourceRepo
              .findById(normalized.id())
              .orElseThrow(
                  () -> new IllegalArgumentException("DataSource not found: " + normalized.id()));
      passwordEnc = existing.getPasswordEnc();
    } else {
      throw new IllegalArgumentException("passwordRaw required");
    }

    DatabaseSource databaseSource =
        DatabaseSource.createUser(
            normalized.name(),
            normalized.dbType(),
            normalized.jdbcUrl(),
            normalized.username(),
            passwordEnc,
            normalized.remarkCustom(),
            normalized.authMode(),
            normalized.connectMode(),
            normalized.host(),
            normalized.port(),
            normalized.databaseName(),
            normalized.serviceName(),
            normalized.sid(),
            normalized.driverClassName(),
            normalized.extraParams());
    try {
      metadataExtractor.testConnection(databaseSource);
      return TestConnectionView.ok();
    } catch (RuntimeException ex) {
      return TestConnectionView.failed(ex.getMessage());
    }
  }

  @Transactional
  public void deleteDatabaseSource(Long id) {
    DatabaseSource databaseSource =
        databaseSourceRepo
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("DataSource not found: " + id));

    BizAssert.state(!databaseSource.isAppSource(), BaseError.FORBIDDEN);
    databaseSchemaRepo.clearDataSourceIdByDataSourceId(id);
    databaseSourceRepo.delete(databaseSource);
  }

  public PageData<DatabaseSource> pageDatabaseSources(
      DatabaseType dbType, String nameLike, PageSpec spec) {
    DatabaseSourcePageQuery query =
        new DatabaseSourcePageQuery(dbType, StrUtil.trimToNull(nameLike));
    return databaseSourceRepo.pageByQuery(query, PageSpecSorts.apply(spec));
  }

  public DatabaseSource getDatabaseSource(Long id) {
    return databaseSourceRepo
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException("DataSource not found: " + id));
  }

  public DatabaseSourceConnectionInfoView getDatabaseSourceConnectionInfo(Long id) {
    DatabaseSource source =
        databaseSourceRepo
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("DataSource not found: " + id));
    String passwordRaw = resolveDatabaseSourcePasswordRaw(source);
    return new DatabaseSourceConnectionInfoView(
        source.getId(),
        source.getName(),
        source.getDbType(),
        source.getAuthMode(),
        source.getConnectMode(),
        source.getHost(),
        source.getPort(),
        source.getDatabaseName(),
        source.getServiceName(),
        source.getSid(),
        source.getDriverClassName(),
        source.getJdbcUrl(),
        source.getUsername(),
        passwordRaw,
        source.getRemarkCustom(),
        source.getExtraParams());
  }

  public List<DatabaseSourceSimpleView> listDatabaseSourcesSimple() {
    return databaseSourceRepo.findAll().stream()
        .map(
            source ->
                new DatabaseSourceSimpleView(
                    source.getId(),
                    source.getName(),
                    source.getDbType(),
                    source.getUsername(),
                    source.getStatus(),
                    source.getSourceType()))
        .toList();
  }

  // --- 数据库管理 ---

  public List<String> listAvailableDatabases(Long dataSourceId) {
    DatabaseSource source =
        databaseSourceRepo
            .findById(dataSourceId)
            .orElseThrow(
                () -> new IllegalArgumentException("DataSource not found: " + dataSourceId));
    return metadataExtractor.listDatabases(source);
  }

  @Transactional
  public Long createDatabaseSchema(
      Long dataSourceId, String databaseName, String alias, String remark) {
    if (dataSourceId == null) {
      throw new IllegalArgumentException("dataSourceId required");
    }
    String normalizedDatabaseName = StrUtil.trimToNull(databaseName);
    if (normalizedDatabaseName == null) {
      throw new IllegalArgumentException("databaseName required");
    }
    String normalizedAlias = StrUtil.trimToNull(alias);
    String normalizedRemark = StrUtil.trimToNull(remark);

    DatabaseSource source =
        databaseSourceRepo
            .findById(dataSourceId)
            .orElseThrow(
                () -> new IllegalArgumentException("DataSource not found: " + dataSourceId));

    assertDatabaseSchemaNotDuplicated(dataSourceId, normalizedDatabaseName, null);

    // 获取基础元数据
    MetadataExtractResult.BaseInfo baseInfo = metadataExtractor.getBaseInfo(source);

    DatabaseSchema schema =
        DatabaseSchema.create(
            dataSourceId, normalizedDatabaseName, normalizedAlias, normalizedRemark);
    schema.updateMetadata(
        baseInfo.productName(),
        baseInfo.productVersion(),
        baseInfo.driverName(),
        baseInfo.driverVersion(),
        baseInfo.fetchedAt());

    try {
      databaseSchemaRepo.save(schema);
    } catch (DataIntegrityViolationException ex) {
      if (isDatabaseSchemaUniqueConflict(ex)) {
        BizAssert.fail(DatabaseSourceError.MANAGED_DATABASE_DUPLICATE);
      }
      throw ex;
    }
    return schema.getId();
  }

  @Transactional
  public void refreshDatabaseSchemaMetadata(Long managedDbId) {
    DatabaseSchema schema =
        databaseSchemaRepo
            .findById(managedDbId)
            .orElseThrow(
                () -> new IllegalArgumentException("Managed database not found: " + managedDbId));
    if (schema.getDataSourceId() == null) {
      throw new IllegalStateException("DataSource not configured");
    }
    DatabaseSource source =
        databaseSourceRepo
            .findById(schema.getDataSourceId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "DataSource not found: " + schema.getDataSourceId()));

    MetadataExtractResult r = metadataExtractor.extract(source, schema.getDatabaseName());

    // 更新数据库层级元数据
    schema.updateMetadata(
        r.baseInfo().productName(),
        r.baseInfo().productVersion(),
        r.baseInfo().driverName(),
        r.baseInfo().driverVersion(),
        r.baseInfo().fetchedAt());
    databaseSchemaRepo.save(schema);

    // 先读出旧表/列，建立 alias 和 remarkCustom 映射用于保留
    List<DatabaseTable> oldTables = tableRepo.findByDatabaseId(schema.getId());
    Map<String, String> tableAliasMap = new HashMap<>();
    Map<String, String> tableRemarkMap = new HashMap<>();
    Map<String, Map<String, String>> colAliasMap = new HashMap<>();
    Map<String, Map<String, String>> colRemarkMap = new HashMap<>();

    for (DatabaseTable t : oldTables) {
      String tableKey = t.getTableName();
      tableAliasMap.put(tableKey, t.getAlias());
      tableRemarkMap.put(tableKey, t.getRemarkCustom());

      List<DatabaseColumn> cols = columnRepo.findByTableId(t.getId());
      Map<String, String> cAlias = new HashMap<>();
      Map<String, String> cRemark = new HashMap<>();
      for (DatabaseColumn col : cols) {
        cAlias.put(col.getColumnName(), col.getAlias());
        cRemark.put(col.getColumnName(), col.getRemarkCustom());
      }
      colAliasMap.put(tableKey, cAlias);
      colRemarkMap.put(tableKey, cRemark);
    }

    // 删除旧数据
    columnRepo.deleteByDatabaseId(schema.getId());
    tableRepo.deleteByDatabaseId(schema.getId());

    // 写入新表
    List<DatabaseTable> newTables = new ArrayList<>();
    for (MetadataExtractResult.TableInfo t : r.tables()) {
      DatabaseTable nt =
          DatabaseTable.create(
              source.getId(),
              schema.getId(),
              t.catalog(),
              t.schema(),
              t.name(),
              t.type(),
              t.remarkDb());

      nt.updateBasicInfo(tableAliasMap.get(t.name()), tableRemarkMap.get(t.name()));
      newTables.add(nt);
    }
    tableRepo.saveAll(newTables);

    // 写入新列
    Map<String, Long> tableIdMap = new HashMap<>();
    for (DatabaseTable t : newTables) {
      tableIdMap.put(t.getTableName(), t.getId());
    }

    List<DatabaseColumn> newColumns = new ArrayList<>();
    for (MetadataExtractResult.ColumnInfo col : r.columns()) {
      Long tableId = tableIdMap.get(col.tableName());
      if (tableId == null) {
        continue;
      }

      DatabaseColumn nc =
          DatabaseColumn.create(
              source.getId(),
              schema.getId(),
              tableId,
              col.columnName(),
              col.typeName(),
              col.jdbcType(),
              col.columnSize(),
              col.decimalDigits(),
              col.nullable(),
              col.ordinalPosition(),
              col.defaultValue(),
              col.remarkDb());

      Map<String, String> oldCAlias = colAliasMap.getOrDefault(col.tableName(), Map.of());
      Map<String, String> oldCRemark = colRemarkMap.getOrDefault(col.tableName(), Map.of());
      nc.updateBasicInfo(oldCAlias.get(col.columnName()), oldCRemark.get(col.columnName()));

      newColumns.add(nc);
    }
    columnRepo.saveAll(newColumns);
  }

  @Transactional
  public void deleteDatabaseSchema(Long id) {
    columnRepo.deleteByDatabaseId(id);
    tableRepo.deleteByDatabaseId(id);
    databaseSchemaRepo.deleteById(id);
  }

  @Transactional
  public void deleteTable(Long tableId) {
    columnRepo.deleteByTableId(tableId);
    tableRepo.deleteById(tableId);
  }

  public List<DatabaseSchema> listDatabaseSchemas(
      Long dataSourceId, boolean unboundOnly, String sortBy, String sortDirection) {
    List<DatabaseSchema> schemas;
    if (unboundOnly) {
      schemas = databaseSchemaRepo.findByDataSourceIdIsNull();
    } else if (dataSourceId == null) {
      schemas = databaseSchemaRepo.findAll();
    } else {
      schemas = databaseSchemaRepo.findByDataSourceId(dataSourceId);
    }

    Comparator<DatabaseSchema> comparator = buildDatabaseSchemaComparator(sortBy, sortDirection);
    return schemas.stream().sorted(comparator).toList();
  }

  @Transactional
  public void updateDatabaseSchemaInfo(Long id, String alias, String remark) {
    DatabaseSchema schema =
        databaseSchemaRepo
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Managed database not found: " + id));
    schema.updateBasicInfo(alias, remark);
    databaseSchemaRepo.save(schema);
  }

  private Comparator<DatabaseSchema> buildDatabaseSchemaComparator(
      String sortBy, String sortDirection) {
    String normalizedSortBy =
        sortBy == null ? "DATA_SOURCE" : sortBy.trim().toUpperCase(Locale.ROOT);
    String normalizedSortDirection =
        sortDirection == null ? "ASC" : sortDirection.trim().toUpperCase(Locale.ROOT);

    Comparator<DatabaseSchema> comparator;
    if ("CREATED_AT".equals(normalizedSortBy)) {
      comparator =
          Comparator.comparing(
                  this::resolveCreatedTime, Comparator.nullsLast(Comparator.naturalOrder()))
              .thenComparing(
                  DatabaseSchema::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    } else {
      comparator =
          Comparator.comparing(
                  DatabaseSchema::getDataSourceId, Comparator.nullsFirst(Comparator.naturalOrder()))
              .thenComparing(
                  DatabaseSchema::getDatabaseName,
                  Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
              .thenComparing(
                  DatabaseSchema::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    if ("DESC".equals(normalizedSortDirection)) {
      return comparator.reversed();
    }
    return comparator;
  }

  private Instant resolveCreatedTime(DatabaseSchema schema) {
    if (schema.getCreatedAt() != null) {
      return schema.getCreatedAt();
    }
    return schema.getFetchedAt();
  }

  public DatabaseSchema getDatabaseSchema(Long id) {
    return databaseSchemaRepo
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Managed database not found: " + id));
  }

  public DatabaseSchemaBasicInfoView getDatabaseSchemaBasicInfo(Long id) {
    DatabaseSchema schema =
        databaseSchemaRepo
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Managed database not found: " + id));
    if (schema.getDataSourceId() == null) {
      throw new IllegalStateException("DataSource not configured");
    }
    DatabaseSource source =
        databaseSourceRepo
            .findById(schema.getDataSourceId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "DataSource not found: " + schema.getDataSourceId()));

    String passwordRaw = resolveDatabaseSourcePasswordRaw(source);
    return new DatabaseSchemaBasicInfoView(
        source.getName(),
        source.getDbType(),
        source.getJdbcUrl(),
        source.getUsername(),
        passwordRaw);
  }

  private String resolveDatabaseSourcePasswordRaw(DatabaseSource source) {
    if (source.isAppSource()) {
      if (StrUtil.isNotBlank(source.getAppDsKey())) {
        String appPassword =
            appDataSourceProvider.findPasswordByKey(source.getAppDsKey()).orElse(null);
        if (StrUtil.isNotBlank(appPassword)) {
          return appPassword;
        }
      }
    }
    if (StrUtil.isNotBlank(source.getPasswordEnc())) {
      return secretCodec.decode(source.getPasswordEnc());
    }
    return null;
  }

  private UpsertConnectionCommand normalizeCommand(UpsertConnectionCommand cmd) {
    if (cmd == null) {
      throw new IllegalArgumentException("command required");
    }
    AuthMode authMode = cmd.authMode() == null ? AuthMode.PASSWORD : cmd.authMode();
    ConnectMode connectMode = cmd.connectMode();
    if (connectMode == null) {
      connectMode =
          cmd.dbType() == DatabaseType.ORACLE ? ConnectMode.SERVICE_NAME : ConnectMode.HOST_PORT;
    }

    Integer port = cmd.port();
    if (port == null) {
      port = resolveDefaultPort(cmd.dbType());
    }
    if (port != null && port <= 0) {
      port = null;
    }

    String driverClassName = StrUtil.trimToNull(cmd.driverClassName());
    if (driverClassName == null) {
      driverClassName = resolveDefaultDriverClassName(cmd.dbType());
    }

    return new UpsertConnectionCommand(
        cmd.id(),
        cmd.name(),
        cmd.dbType(),
        authMode,
        connectMode,
        StrUtil.trimToNull(cmd.host()),
        port,
        StrUtil.trimToNull(cmd.databaseName()),
        StrUtil.trimToNull(cmd.serviceName()),
        StrUtil.trimToNull(cmd.sid()),
        driverClassName,
        cmd.jdbcUrl(),
        cmd.username(),
        cmd.passwordRaw(),
        cmd.defaultSchema(),
        cmd.remarkCustom(),
        StrUtil.trimToNull(cmd.extraParams()));
  }

  private Integer resolveDefaultPort(DatabaseType dbType) {
    if (dbType == null) {
      return null;
    }
    return switch (dbType) {
      case MYSQL -> 3306;
      case POSTGRESQL -> 5432;
      case ORACLE -> 1521;
      case SQLSERVER -> 1433;
      case H2 -> null;
    };
  }

  private String resolveDefaultDriverClassName(DatabaseType dbType) {
    if (dbType == null) {
      return null;
    }
    return switch (dbType) {
      case MYSQL -> "com.mysql.cj.jdbc.Driver";
      case POSTGRESQL -> "org.postgresql.Driver";
      case ORACLE -> "oracle.jdbc.OracleDriver";
      case SQLSERVER -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
      case H2 -> "org.h2.Driver";
    };
  }

  private void validateUserConnectionCommand(UpsertConnectionCommand cmd) {
    if (cmd.dbType() == null) {
      throw new IllegalArgumentException("dbType required");
    }
    if (!USER_SUPPORTED_DB_TYPES.contains(cmd.dbType())) {
      throw new IllegalArgumentException("Only MYSQL/POSTGRESQL/ORACLE are supported");
    }
    if (cmd.authMode() == null) {
      throw new IllegalArgumentException("authMode required");
    }
    if (cmd.connectMode() == null) {
      throw new IllegalArgumentException("connectMode required");
    }
    if (cmd.dbType() != DatabaseType.ORACLE && cmd.connectMode() != ConnectMode.HOST_PORT) {
      throw new IllegalArgumentException("connectMode not supported for current dbType");
    }
    if (StrUtil.isBlank(cmd.driverClassName())) {
      throw new IllegalArgumentException("driverClassName required");
    }
    if (StrUtil.isBlank(cmd.host())) {
      throw new IllegalArgumentException("host required");
    }
    if (cmd.port() == null) {
      throw new IllegalArgumentException("port required");
    }
    if (cmd.connectMode() == ConnectMode.HOST_PORT && StrUtil.isBlank(cmd.databaseName())) {
      throw new IllegalArgumentException("databaseName required");
    }
    if (cmd.connectMode() == ConnectMode.SERVICE_NAME && StrUtil.isBlank(cmd.serviceName())) {
      throw new IllegalArgumentException("serviceName required");
    }
    if (cmd.connectMode() == ConnectMode.SID && StrUtil.isBlank(cmd.sid())) {
      throw new IllegalArgumentException("sid required");
    }
    if (cmd.connectMode() == ConnectMode.TNS && StrUtil.isBlank(cmd.extraParams())) {
      throw new IllegalArgumentException("extraParams required for TNS");
    }
    if (StrUtil.isBlank(cmd.jdbcUrl())) {
      throw new IllegalArgumentException("jdbcUrl required");
    }

    String jdbcUrl = cmd.jdbcUrl().trim();
    String lower = jdbcUrl.toLowerCase(Locale.ROOT);
    switch (cmd.dbType()) {
      case MYSQL -> {
        if (!lower.startsWith("jdbc:mysql://")) {
          throw new IllegalArgumentException("Invalid MySQL jdbcUrl");
        }
      }
      case POSTGRESQL -> {
        if (!lower.startsWith("jdbc:postgresql://")) {
          throw new IllegalArgumentException("Invalid PostgreSQL jdbcUrl");
        }
      }
      case ORACLE -> {
        if (lower.contains("jdbc:oracle:oci") || lower.contains("jdbc:oracle:oci8")) {
          throw new IllegalArgumentException("Oracle OCI/OCI8 is not supported currently");
        }
      }
      default -> throw new IllegalArgumentException("Unsupported dbType");
    }
  }

  @Transactional
  public void rebindDatabaseSchema(Long id, Long dataSourceId) {
    if (dataSourceId == null) {
      throw new IllegalArgumentException("dataSourceId required");
    }
    DatabaseSource source =
        databaseSourceRepo
            .findById(dataSourceId)
            .orElseThrow(
                () -> new IllegalArgumentException("DataSource not found: " + dataSourceId));
    DatabaseSchema schema =
        databaseSchemaRepo
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Managed database not found: " + id));

    String normalizedDatabaseName = StrUtil.trimToNull(schema.getDatabaseName());
    if (normalizedDatabaseName == null) {
      throw new IllegalStateException("Managed database name is empty");
    }
    assertDatabaseSchemaNotDuplicated(source.getId(), normalizedDatabaseName, schema.getId());

    schema.rebindDataSource(source.getId());
    try {
      databaseSchemaRepo.save(schema);
    } catch (DataIntegrityViolationException ex) {
      if (isDatabaseSchemaUniqueConflict(ex)) {
        BizAssert.fail(DatabaseSourceError.MANAGED_DATABASE_DUPLICATE);
      }
      throw ex;
    }
  }

  private void assertDatabaseSchemaNotDuplicated(
      Long dataSourceId, String databaseName, Long excludedManagedDbId) {
    databaseSchemaRepo
        .findByDataSourceIdAndDatabaseName(dataSourceId, databaseName)
        .filter(existing -> !Objects.equals(existing.getId(), excludedManagedDbId))
        .ifPresent(_existing -> BizAssert.fail(DatabaseSourceError.MANAGED_DATABASE_DUPLICATE));
  }

  private boolean isDatabaseSchemaUniqueConflict(DataIntegrityViolationException ex) {
    StringBuilder sb = new StringBuilder();
    if (ex.getMessage() != null) {
      sb.append(ex.getMessage());
    }
    Throwable cause = ex.getCause();
    while (cause != null) {
      if (cause.getMessage() != null) {
        sb.append(' ').append(cause.getMessage());
      }
      cause = cause.getCause();
    }

    String msg = sb.toString().toLowerCase(Locale.ROOT);
    if (msg.contains("uk_db_schema_name")) {
      return true;
    }
    return msg.contains("db_schema")
        && (msg.contains("duplicate") || msg.contains("unique") || msg.contains("constraint"));
  }

  private String resolvePasswordEnc(UpsertConnectionCommand cmd, DatabaseSource existing) {
    if (cmd.passwordRaw() != null && !cmd.passwordRaw().isBlank()) {
      return secretCodec.encode(cmd.passwordRaw());
    }
    return Objects.requireNonNull(existing.getPasswordEnc(), "passwordEnc required");
  }

  // --- 表与列管理 ---

  @Transactional
  public void updateTableInfo(Long tableId, String alias, String remark) {
    DatabaseTable t =
        tableRepo
            .findById(tableId)
            .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableId));
    t.updateBasicInfo(alias, remark);
    tableRepo.save(t);
  }

  @Transactional
  public void updateColumnInfo(Long columnId, String alias, String remark) {
    DatabaseColumn c =
        columnRepo
            .findById(columnId)
            .orElseThrow(() -> new IllegalArgumentException("Column not found: " + columnId));
    c.updateBasicInfo(alias, remark);
    columnRepo.save(c);
  }

  public PageData<DatabaseTable> pageTables(
      Long managedDbId, String schema, String nameLike, String tableType, PageSpec spec) {
    if (managedDbId == null) {
      throw new IllegalArgumentException("managedDbId required");
    }
    DatabaseTablePageQuery query =
        new DatabaseTablePageQuery(
            managedDbId,
            StrUtil.trimToNull(schema),
            StrUtil.trimToNull(nameLike),
            StrUtil.trimToNull(tableType));
    return tableRepo.pageByQuery(query, PageSpecSorts.apply(spec));
  }

  public List<DatabaseTable> listAllTables(Long managedDbId) {
    return tableRepo.findByDatabaseId(managedDbId);
  }

  public List<DatabaseColumn> listAllColumns(Long tableId) {
    return columnRepo.findByTableId(tableId);
  }

  public PageData<DatabaseColumn> pageColumns(Long tableId, String nameLike, PageSpec spec) {
    return columnRepo.findByTableIdAndColumnNameContainingIgnoreCase(
        tableId, StrUtil.trimToNull(nameLike), PageSpecSorts.apply(spec));
  }
}

package com.corwin.schemaforge.application.service;

import com.corwin.datasource.domain.model.DatabaseSchema;
import com.corwin.datasource.domain.model.DatabaseSource;
import com.corwin.datasource.domain.model.DatabaseTable;
import com.corwin.datasource.domain.model.DatabaseType;
import com.corwin.datasource.domain.repo.DatabaseSchemaRepository;
import com.corwin.datasource.domain.repo.DatabaseSourceRepository;
import com.corwin.datasource.domain.repo.DatabaseTableRepository;
import com.corwin.datasource.infrastructure.jdbc.JdbcConnectionFactory;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.framework.mybatis.LikePatternUtils;
import com.corwin.framework.util.SignUtil;
import com.corwin.framework.util.StrUtil;
import com.corwin.framework.web.response.PageResult;
import com.corwin.framework.web.sort.PageSpecSorts;
import com.corwin.schemaforge.application.command.CreateSchemaSnapshotCommand;
import com.corwin.schemaforge.application.command.UpdateSchemaSnapshotInfoCommand;
import com.corwin.schemaforge.application.model.SnapshotObjectSelection;
import com.corwin.schemaforge.application.view.SnapshotSelectableObjectView;
import com.corwin.schemaforge.domain.model.DatabaseSnapshot;
import com.corwin.schemaforge.domain.repo.DatabaseSnapshotRepository;
import com.corwin.schemaforge.infrastructure.liquibase.LiquibaseEngine;
import com.corwin.schemaforge.infrastructure.persistence.SchemaSnapshotMybatisMapper;
import com.corwin.schemaforge.interfaces.web.res.SchemaSnapshotRes;
import com.corwin.system.file.application.port.FileCommandPort;
import com.corwin.system.file.published.FilePurpose;
import com.corwin.system.file.published.InternalFileType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Corwin 2026/2/24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaSnapshotAppService {

    private final DatabaseSnapshotRepository databaseSnapshotRepository;
    private final DatabaseSchemaRepository databaseSchemaRepository;
    private final DatabaseSourceRepository databaseSourceRepository;
    private final DatabaseTableRepository databaseTableRepository;
    private final JdbcConnectionFactory jdbcConnectionFactory;
    private final LiquibaseEngine liquibaseEngine;
    private final FileCommandPort fileService;
    private final SchemaSnapshotMybatisMapper schemaSnapshotMybatisMapper;

    @Transactional
    public String createSnapshot(CreateSchemaSnapshotCommand command) {
        BizAssert.notNull(command, BaseError.MISSING_PARAMETER);
        BizAssert.notNull(command.managedDatabaseId(), BaseError.MISSING_PARAMETER);

        String snapshotName = StrUtil.trimToNull(command.name());
        BizAssert.notBlank(snapshotName, BaseError.MISSING_PARAMETER);
        String remark = StrUtil.trimToNull(command.remark());

        DatabaseSchema managedDatabase = databaseSchemaRepository.findById(command.managedDatabaseId()).orElseThrow(
                () -> new BizException("Managed database not found: " + command.managedDatabaseId(),
                        BaseError.NOT_FOUND));

        Long dataSourceId = managedDatabase.getDataSourceId();
        BizAssert.notNull(dataSourceId, BaseError.CONFLICT);

        DatabaseSource dataSource = databaseSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new BizException("DataSource not found: " + dataSourceId, BaseError.NOT_FOUND));

        List<DatabaseTable> availableObjects = databaseTableRepository.findByDatabaseId(managedDatabase.getId());
        SnapshotObjectSelection selectedObjects = resolveSnapshotObjectSelection(command.selectedObjectIds(),
                availableObjects);
        BizAssert.state(
                !selectedObjects.selectedTableNames().isEmpty() || !selectedObjects.selectedViewNames().isEmpty(),
                BaseError.MISSING_PARAMETER);

        LiquibaseEngine.SnapshotResult snapshotResult;
        LiquibaseEngine.DdlResult ddlResult;
        try (Connection connection = jdbcConnectionFactory.openConnection(dataSource)) {
            DatabaseScopeState scopeState = applyManagedDatabaseScope(connection, managedDatabase, dataSource);
            try {
                snapshotResult = liquibaseEngine.generateSnapshot(connection, selectedObjects.selectedTableNames(),
                        selectedObjects.selectedViewNames());
                ddlResult = liquibaseEngine.generateDdl(connection, selectedObjects.selectedTableNames(),
                        selectedObjects.selectedViewNames());
            } finally {
                restoreManagedDatabaseScope(connection, scopeState);
            }
        } catch (SQLException e) {
            throw new BizException("Open database connection failed", BaseError.DB_ERROR);
        }

        byte[] content = snapshotResult.content();
        String logicalFileId = fileService.createInternalFile(snapshotName, InternalFileType.YAML, content,
                FilePurpose.SNAPSHOT);
        String sqlLogicalFileId = fileService.createInternalFile(snapshotName, InternalFileType.SQL,
                ddlResult.content(), FilePurpose.SNAPSHOT);

        DatabaseSnapshot snapshot = new DatabaseSnapshot(managedDatabase.getId(), snapshotName, remark,
                snapshotResult.dbType(), snapshotResult.dbVersion(), resolveSchemaName(snapshotResult, managedDatabase),
                logicalFileId, sqlLogicalFileId, calculateHash(content));
        databaseSnapshotRepository.save(snapshot);
        return snapshot.getId();
    }

    public List<SnapshotSelectableObjectView> listSnapshotSelectableObjects(Long managedDatabaseId) {
        BizAssert.notNull(managedDatabaseId, BaseError.MISSING_PARAMETER);
        databaseSchemaRepository.findById(managedDatabaseId).orElseThrow(
                () -> new BizException("Managed database not found: " + managedDatabaseId, BaseError.NOT_FOUND));
        return databaseTableRepository.findByDatabaseId(managedDatabaseId).stream()
                .sorted(Comparator.comparingInt(this::tableTypeOrder)
                        .thenComparing(item -> normalizeTableType(item.getTableType()))
                        .thenComparing(item -> normalize(item.getTableSchema()))
                        .thenComparing(item -> normalize(item.getTableName())))
                .map(item -> new SnapshotSelectableObjectView(item.getId(), item.getTableName(), item.getTableSchema(),
                        item.getTableType(), item.getAlias())).toList();
    }

    public PageResult<SchemaSnapshotRes> pageQuery(Long managedDatabaseId, String nameLike, PageSpec spec) {
        PageSpec spec0 = PageSpecSorts.apply(spec);
        String nameLike0 = LikePatternUtils.toContainsPattern(nameLike);
        PageData<SchemaSnapshotRes> page = schemaSnapshotMybatisMapper.pageQuery(managedDatabaseId, nameLike0, spec0);
        return PageResult.of(page);
    }

    public List<SchemaSnapshotRes> listAll() {
        return schemaSnapshotMybatisMapper.listAll();
    }

    @Transactional
    public void delete(String id) {
        BizAssert.notBlank(id, BaseError.MISSING_PARAMETER);
        DatabaseSnapshot snapshot = databaseSnapshotRepository.findById(id)
                .orElseThrow(() -> new BizException("Snapshot not found: " + id, BaseError.NOT_FOUND));

        databaseSnapshotRepository.delete(snapshot);
        fileService.deleteFile(snapshot.getLogicalFileId());
        if (StrUtil.isNotBlank(snapshot.getSqlLogicalFileId())) {
            fileService.deleteFile(snapshot.getSqlLogicalFileId());
        }
    }

    @Transactional
    public void updateInfo(UpdateSchemaSnapshotInfoCommand command) {
        BizAssert.notNull(command, BaseError.MISSING_PARAMETER);
        BizAssert.notBlank(command.id(), BaseError.MISSING_PARAMETER);

        String snapshotName = StrUtil.trimToNull(command.name());
        BizAssert.notBlank(snapshotName, BaseError.MISSING_PARAMETER);
        String remark = StrUtil.trimToNull(command.remark());

        DatabaseSnapshot snapshot = databaseSnapshotRepository.findById(command.id())
                .orElseThrow(() -> new BizException("Snapshot not found: " + command.id(), BaseError.NOT_FOUND));
        snapshot.updateInfo(snapshotName, remark);
        databaseSnapshotRepository.save(snapshot);
    }

    private DatabaseScopeState applyManagedDatabaseScope(Connection connection, DatabaseSchema managedDatabase,
            DatabaseSource dataSource) throws SQLException {
        String databaseName = StrUtil.trimToNull(managedDatabase.getDatabaseName());
        if (databaseName == null) {
            return DatabaseScopeState.none();
        }
        DatabaseType dbType = dataSource.getDbType();
        boolean shouldRestore = dataSource.isAppSource();
        if (usesCatalog(dbType)) {
            String originalCatalog = StrUtil.trimToNull(connection.getCatalog());
            ensureCanRestoreScopeIfNeeded(shouldRestore, originalCatalog, dataSource, "catalog");
            connection.setCatalog(databaseName);
            return DatabaseScopeState.forCatalog(shouldRestore, originalCatalog);
        }
        String originalSchema = StrUtil.trimToNull(connection.getSchema());
        ensureCanRestoreScopeIfNeeded(shouldRestore, originalSchema, dataSource, "schema");
        connection.setSchema(databaseName);
        return DatabaseScopeState.forSchema(shouldRestore, originalSchema);
    }

    private void ensureCanRestoreScopeIfNeeded(boolean shouldRestore, String originalScope, DatabaseSource dataSource,
            String scopeKind) {
        if (!shouldRestore) {
            return;
        }
        if (originalScope != null) {
            return;
        }
        throw new BizException(
                "应用数据源未配置默认" + scopeKind + "，无法安全执行快照并恢复连接作用域: " + dataSource.getName(),
                BaseError.CONFLICT);
    }

    private void restoreManagedDatabaseScope(Connection connection, DatabaseScopeState scopeState) {
        if (scopeState == null || !scopeState.scoped() || !scopeState.shouldRestore()) {
            return;
        }
        try {
            if (scopeState.usesCatalog()) {
                if (scopeState.originalCatalog() == null) {
                    return;
                }
                connection.setCatalog(scopeState.originalCatalog());
            } else {
                if (scopeState.originalSchema() == null) {
                    return;
                }
                connection.setSchema(scopeState.originalSchema());
            }
        } catch (SQLException e) {
            log.warn("恢复连接数据库作用域失败", e);
        }
    }

    private boolean usesCatalog(DatabaseType dbType) {
        return dbType == DatabaseType.MYSQL || dbType == DatabaseType.SQLSERVER;
    }

    private record DatabaseScopeState(
            boolean scoped,
            boolean shouldRestore,
            boolean usesCatalog,
            String originalCatalog,
            String originalSchema
    ) {
        private static DatabaseScopeState none() {
            return new DatabaseScopeState(false, false, false, null, null);
        }

        private static DatabaseScopeState forCatalog(boolean shouldRestore, String originalCatalog) {
            return new DatabaseScopeState(true, shouldRestore, true, originalCatalog, null);
        }

        private static DatabaseScopeState forSchema(boolean shouldRestore, String originalSchema) {
            return new DatabaseScopeState(true, shouldRestore, false, null, originalSchema);
        }
    }

    private String resolveSchemaName(LiquibaseEngine.SnapshotResult snapshotResult, DatabaseSchema managedDatabase) {
        String schemaName = StrUtil.trimToNull(snapshotResult.schemaName());
        if (schemaName != null) {
            return schemaName;
        }
        return StrUtil.trimToNull(managedDatabase.getDatabaseName());
    }

    private String calculateHash(byte[] content) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
            return SignUtil.sha256(inputStream);
        } catch (IOException e) {
            throw new BizException("Calculate snapshot hash failed", BaseError.SERVICE_ERROR);
        }
    }

    private SnapshotObjectSelection resolveSnapshotObjectSelection(List<Long> selectedObjectIds,
            List<DatabaseTable> availableObjects) {
        if (availableObjects == null || availableObjects.isEmpty()) {
            return SnapshotObjectSelection.empty();
        }

        Map<Long, DatabaseTable> objectMap = availableObjects.stream()
                .collect(Collectors.toMap(DatabaseTable::getId, Function.identity()));
        List<DatabaseTable> selectedObjects;
        if (selectedObjectIds == null) {
            selectedObjects = availableObjects;
        } else {
            selectedObjects = new ArrayList<>();
            for (Long objectId : selectedObjectIds) {
                BizAssert.notNull(objectId, BaseError.MISSING_PARAMETER);
                DatabaseTable item = objectMap.get(objectId);
                if (item == null) {
                    throw new BizException("Snapshot object not found: " + objectId, BaseError.NOT_FOUND);
                }
                selectedObjects.add(item);
            }
        }

        Set<String> selectedTables = new LinkedHashSet<>();
        Set<String> selectedViews = new LinkedHashSet<>();
        for (DatabaseTable item : selectedObjects) {
            String tableName = StrUtil.trimToNull(item.getTableName());
            if (tableName == null) {
                continue;
            }
            if (isView(item.getTableType())) {
                selectedViews.add(tableName);
            } else {
                selectedTables.add(tableName);
            }
        }
        return new SnapshotObjectSelection(selectedTables, selectedViews);
    }

    private boolean isView(String tableType) {
        return "VIEW".equalsIgnoreCase(normalizeTableType(tableType));
    }

    private String normalizeTableType(String tableType) {
        return normalize(tableType).toUpperCase(Locale.ROOT);
    }

    private String normalize(String text) {
        return StrUtil.trimToEmpty(text);
    }

    private int tableTypeOrder(DatabaseTable table) {
        return isView(table.getTableType()) ? 1 : 0;
    }

}

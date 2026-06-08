package com.corwin.schemaforge.application.service;

import com.corwin.datasource.domain.model.DatabaseSchema;
import com.corwin.datasource.domain.model.DatabaseSource;
import com.corwin.datasource.domain.model.DatabaseType;
import com.corwin.datasource.domain.repo.DatabaseSchemaRepository;
import com.corwin.datasource.domain.repo.DatabaseSourceRepository;
import com.corwin.datasource.infrastructure.jdbc.JdbcConnectionFactory;
import com.corwin.schemaforge.application.view.DiffResultView;
import com.corwin.schemaforge.infrastructure.liquibase.LiquibaseEngine;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.framework.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Corwin 2026/2/24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaDiffAppService {

    private final DatabaseSchemaRepository databaseSchemaRepository;
    private final DatabaseSourceRepository databaseSourceRepository;
    private final JdbcConnectionFactory jdbcConnectionFactory;
    private final LiquibaseEngine liquibaseEngine;

    public DiffResultView compare(Long referenceManagedDatabaseId, Long targetManagedDatabaseId) {
        BizAssert.notNull(referenceManagedDatabaseId, BaseError.MISSING_PARAMETER);
        BizAssert.notNull(targetManagedDatabaseId, BaseError.MISSING_PARAMETER);

        DatabaseSchema referenceManagedDatabase = resolveDatabaseSchema(referenceManagedDatabaseId);
        DatabaseSchema targetManagedDatabase = resolveDatabaseSchema(targetManagedDatabaseId);

        DatabaseSource referenceDataSource = resolveDatabaseSource(referenceManagedDatabase);
        DatabaseSource targetDataSource = resolveDatabaseSource(targetManagedDatabase);

        try (Connection referenceConnection = jdbcConnectionFactory.openConnection(referenceDataSource);
             Connection targetConnection = jdbcConnectionFactory.openConnection(targetDataSource)) {
            DatabaseScopeState referenceScope = applyManagedDatabaseScope(referenceConnection, referenceManagedDatabase,
                    referenceDataSource);
            DatabaseScopeState targetScope = applyManagedDatabaseScope(targetConnection, targetManagedDatabase,
                    targetDataSource);

            try {
                LiquibaseEngine.CompareResult compareResult = liquibaseEngine.compare(referenceConnection,
                        targetConnection);
                return new DiffResultView(compareResult.addedCount(), compareResult.removedCount(),
                        compareResult.changedCount(), compareResult.changeLogXml(), compareResult.changeSql());
            } finally {
                restoreManagedDatabaseScope(targetConnection, targetScope);
                restoreManagedDatabaseScope(referenceConnection, referenceScope);
            }
        } catch (SQLException e) {
            throw new BizException("Open database connection failed", BaseError.DB_ERROR);
        }
    }

    private DatabaseSchema resolveDatabaseSchema(Long managedDatabaseId) {
        return databaseSchemaRepository.findById(managedDatabaseId).orElseThrow(
                () -> new BizException("Managed database not found: " + managedDatabaseId, BaseError.NOT_FOUND));
    }

    private DatabaseSource resolveDatabaseSource(DatabaseSchema managedDatabase) {
        Long dataSourceId = managedDatabase.getDataSourceId();
        BizAssert.notNull(dataSourceId, BaseError.CONFLICT);
        return databaseSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new BizException("DataSource not found: " + dataSourceId, BaseError.NOT_FOUND));
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
                "应用数据源未配置默认" + scopeKind + "，无法安全执行结构对比并恢复连接作用域: " + dataSource.getName(),
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
            boolean scoped, boolean shouldRestore, boolean usesCatalog, String originalCatalog, String originalSchema
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
}

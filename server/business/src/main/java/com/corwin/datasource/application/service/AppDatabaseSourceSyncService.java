package com.corwin.datasource.application.service;

import com.corwin.datasource.application.port.AppDataSourceProvider;
import com.corwin.datasource.application.port.MetadataExtractor;
import com.corwin.datasource.domain.model.DatabaseSource;
import com.corwin.datasource.domain.model.DatabaseType;
import com.corwin.datasource.domain.repo.DatabaseSchemaRepository;
import com.corwin.datasource.domain.repo.DatabaseSourceRepository;
import com.corwin.framework.util.HighDate;
import com.corwin.framework.util.StrUtil;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.sql.Wrapper;
import java.time.Instant;
import java.util.*;

/**
 * @author Corwin 2026/2/7
 */
@Service
@Slf4j
@AllArgsConstructor
public class AppDatabaseSourceSyncService {

    private final DatabaseSourceRepository databaseSourceRepo;
    private final DatabaseSchemaRepository databaseSchemaRepo;
    private final AppDataSourceProvider appDataSourceProvider;
    private final MetadataExtractor metadataExtractor;

    @Transactional
    public void syncAppDataSources() {
        Map<String, DataSource> dataSources = appDataSourceProvider.listAll();
        Instant startupTime = HighDate.mockInstant();
        Set<String> beanKeys = new HashSet<>(dataSources.keySet());
        cleanupMissingAppSources(beanKeys);
        if (dataSources.isEmpty()) {
            log.info("未发现应用数据源 Bean，已完成清理检查。");
            return;
        }
        for (Map.Entry<String, DataSource> entry : dataSources.entrySet()) {
            syncSingle(entry.getKey(), entry.getValue(), startupTime);
        }
    }

    private void syncSingle(String beanName, DataSource rawDataSource, Instant startupTime) {
        AppDataSourceSpec spec = resolveSpec(beanName, rawDataSource);
        if (spec == null) {
            return;
        }

        DatabaseSource existing = databaseSourceRepo.findByAppDsKey(beanName).orElseGet(
                () -> databaseSourceRepo.findByDbTypeAndJdbcUrlAndUsername(spec.dbType(), spec.jdbcUrl(),
                        spec.username()).filter(DatabaseSource::isAppSource).orElse(null));
        if (existing != null && isSameConnection(existing, spec)) {
            existing.updateAppConfig(existing.getName(), spec.dbType(), spec.jdbcUrl(), spec.username(), beanName);
            testAppDataSource(existing, startupTime);
            databaseSourceRepo.save(existing);
            return;
        }

        if (existing != null) {
            detachAndDelete(existing);
        }

        String name = existing != null ? existing.getName() : buildName(spec);
        name = ensureUniqueName(name, existing != null ? existing.getId() : null);
        DatabaseSource created = DatabaseSource.createApp(name, spec.dbType(), spec.jdbcUrl(), spec.username(),
                beanName, null);
        testAppDataSource(created, startupTime);
        databaseSourceRepo.save(created);
    }

    private void testAppDataSource(DatabaseSource source, Instant startupTime) {
        try {
            metadataExtractor.testConnection(source);
            source.markTestOk(startupTime);
        } catch (RuntimeException ex) {
            String msg = "应用数据源连接失败: name=" + source.getName() + ", jdbcUrl=" + source.getJdbcUrl() + ", error=" + ex.getMessage();
            throw new RuntimeException(msg, ex);
        }
    }

    private void detachAndDelete(DatabaseSource existing) {
        databaseSchemaRepo.clearDataSourceIdByDataSourceId(existing.getId());
        databaseSourceRepo.delete(existing);
    }

    private void cleanupMissingAppSources(Set<String> beanKeys) {
        List<DatabaseSource> appSources = databaseSourceRepo.findAll().stream().filter(DatabaseSource::isAppSource)
                .toList();
        for (DatabaseSource source : appSources) {
            String key = source.getAppDsKey();
            if (StrUtil.isBlank(key) || !beanKeys.contains(key)) {
                log.info("应用数据源不存在，已清理：name={}, appDsKey={}", source.getName(), key);
                detachAndDelete(source);
            }
        }
    }

    private boolean isSameConnection(DatabaseSource existing, AppDataSourceSpec spec) {
        return existing.getDbType() == spec.dbType() && Objects.equals(existing.getJdbcUrl(),
                spec.jdbcUrl()) && Objects.equals(existing.getUsername(), spec.username());
    }

    private AppDataSourceSpec resolveSpec(String beanName, DataSource dataSource) {
        UnwrapResult unwrapResult = unwrapWithChain(dataSource);
        DataSource target = unwrapResult.dataSource();
        String jdbcUrl = resolveJdbcUrl(target);
        String username = resolveUsername(target);
        if (StrUtil.isBlank(jdbcUrl) || StrUtil.isBlank(username)) {
            log.warn("应用数据源信息不足，已跳过：beanName={}, chain={}", beanName, unwrapResult.chain());
            return null;
        }
        DatabaseType dbType = DatabaseType.fromJdbcUrl(jdbcUrl);
        if (dbType == null) {
            log.warn("无法识别数据库类型，已跳过：beanName={}, jdbcUrl={}, chain={}", beanName, jdbcUrl,
                    unwrapResult.chain());
            return null;
        }
        return new AppDataSourceSpec(beanName, jdbcUrl, username, dbType);
    }

    private UnwrapResult unwrapWithChain(DataSource dataSource) {
        if (dataSource == null) {
            return new UnwrapResult(null, "");
        }
        List<String> chain = new ArrayList<>();
        Set<DataSource> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        DataSource current = dataSource;
        int depth = 0;
        while (depth < 16 && visited.add(current)) {
            chain.add(current.getClass().getName());
            DataSource next = unwrapOnce(current);
            if (next == null || next == current) {
                break;
            }
            current = next;
            depth++;
        }
        return new UnwrapResult(current, String.join(" -> ", chain));
    }

    private DataSource unwrapOnce(DataSource dataSource) {
        if (dataSource instanceof DelegatingDataSource delegating) {
            DataSource target = delegating.getTargetDataSource();
            if (target != null) {
                return target;
            }
        }
        if (dataSource instanceof Wrapper wrapper) {
            try {
                if (wrapper.isWrapperFor(HikariDataSource.class)) {
                    return wrapper.unwrap(HikariDataSource.class);
                }
                if (wrapper.isWrapperFor(DataSource.class)) {
                    DataSource unwrapped = wrapper.unwrap(DataSource.class);
                    if (unwrapped != null && unwrapped != dataSource) {
                        return unwrapped;
                    }
                }
            } catch (Exception ex) {
                return dataSource;
            }
        }
        for (String methodName : unwrapMethodNames()) {
            Optional<DataSource> unwrapped = readDataSourceProperty(dataSource, methodName);
            if (unwrapped.isPresent() && unwrapped.get() != dataSource) {
                return unwrapped.get();
            }
        }
        return dataSource;
    }

    private String resolveJdbcUrl(Object dataSource) {
        if (dataSource instanceof HikariDataSource hikari) {
            return hikari.getJdbcUrl();
        }
        return readStringProperty(dataSource, "getJdbcUrl").orElseGet(
                () -> readStringProperty(dataSource, "getUrl").orElse(null));
    }

    private String resolveUsername(Object dataSource) {
        if (dataSource instanceof HikariDataSource hikari) {
            return hikari.getUsername();
        }
        return readStringProperty(dataSource, "getUsername").orElseGet(
                () -> readStringProperty(dataSource, "getUser").orElse(null));
    }

    private Optional<String> readStringProperty(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            if (method.getReturnType() != String.class) {
                return Optional.empty();
            }
            return Optional.ofNullable((String) method.invoke(target));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private Optional<DataSource> readDataSourceProperty(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            if (!DataSource.class.isAssignableFrom(method.getReturnType())) {
                return Optional.empty();
            }
            return Optional.ofNullable((DataSource) method.invoke(target));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private List<String> unwrapMethodNames() {
        return List.of("getTargetDataSource", "getDelegate", "getDataSource", "getRealDataSource",
                "getWrappedDataSource");
    }

    private String buildName(AppDataSourceSpec spec) {
        return "app-" + spec.dbType().name().toLowerCase() + "-" + spec.beanName();
    }

    private String ensureUniqueName(String base, Long ignoreId) {
        String candidate = base;
        int index = 2;
        while (true) {
            Optional<DatabaseSource> existing = databaseSourceRepo.findByName(candidate);
            if (existing.isEmpty() || Objects.equals(existing.get().getId(), ignoreId)) {
                return candidate;
            }
            candidate = base + "-auto-" + index;
            index++;
        }
    }

    private record AppDataSourceSpec(
            String beanName,
            String jdbcUrl,
            String username,
            DatabaseType dbType
    ) {
    }

    private record UnwrapResult(
            DataSource dataSource,
            String chain
    ) {
    }
}

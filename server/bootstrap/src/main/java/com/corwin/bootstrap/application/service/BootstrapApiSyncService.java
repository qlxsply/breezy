package com.corwin.bootstrap.application.service;

import com.corwin.bootstrap.application.BootstrapTaskKey;
import com.corwin.bootstrap.application.BootstrapTaskReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Corwin 2026/5/5
 */
@Service
@RequiredArgsConstructor
public class BootstrapApiSyncService {

    private final DataSource dataSource;
    private final BootstrapSqlTemplateService sqlTemplateService;
    private final BootstrapControllerApiScanner controllerApiScanner;

    public BootstrapTaskReport run(boolean dryRun) {
        long startedAt = System.currentTimeMillis();
        BootstrapControllerApiScanner.ScanResult scanResult = controllerApiScanner.scan();
        List<BootstrapControllerApiScanner.ApiSeed> apis = scanResult.apis();
        List<BootstrapControllerApiScanner.PermissionSeed> permissions = scanResult.permissions();

        Set<String> existingPermissionCodes = BootstrapJdbcTransactionSupport.execute(dataSource,
                connection -> loadExistingPermissionCodes(connection,
                        sqlTemplateService.load("api_select_permission_codes.sql")));
        Map<String, String> existingSortOptions = BootstrapJdbcTransactionSupport.execute(dataSource,
                connection -> loadExistingSortOptions(connection,
                        sqlTemplateService.load("api_select_sort_options.sql")));
        List<String> createdPermissionCodes = permissions.stream()
                .map(BootstrapControllerApiScanner.PermissionSeed::code)
                .filter(code -> !existingPermissionCodes.contains(code)).toList();
        List<String> orphanPermissionCodes = existingPermissionCodes.stream()
                .filter(code -> permissions.stream().noneMatch(item -> item.code().equals(code))).toList();

        if (!dryRun) {
            BootstrapJdbcTransactionSupport.executeWithoutResult(dataSource, connection -> {
                executeUpdate(connection, sqlTemplateService.load("api_delete_api_permissions.sql"));
                executeUpdate(connection, sqlTemplateService.load("api_delete_apis.sql"));
                batchUpsertPermissions(connection, permissions);
                batchInsertApis(connection, apis, existingSortOptions);
                batchInsertApiPermissions(connection, apis);
            });
        }

        int permissionLinkCount = apis.stream().mapToInt(api -> api.permissionCodes().size()).sum();
        String message = "apis=" + apis.size() + "; permissions=" + permissions.size() + "; permissionLinks=" + permissionLinkCount + "; createdPermissions=" + createdPermissionCodes.size() + "; orphanPermissions=" + orphanPermissionCodes.size() + "; strategy=rebuild-api-and-relink-permissions";
        return new BootstrapTaskReport(BootstrapTaskKey.API_SYNC, dryRun, true, System.currentTimeMillis() - startedAt,
                message);
    }

    private Set<String> loadExistingPermissionCodes(Connection connection, String sql) throws SQLException {
        Set<String> result = new LinkedHashSet<>();
        try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(rs.getString(1));
            }
        }
        return result;
    }

    private Map<String, String> loadExistingSortOptions(Connection connection, String sql) throws SQLException {
        Map<String, String> result = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String key = apiKey(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4));
                result.put(key, rs.getString(5));
            }
        }
        return result;
    }

    private void batchUpsertPermissions(Connection connection,
            List<BootstrapControllerApiScanner.PermissionSeed> permissions) throws SQLException {
        if (permissions.isEmpty()) {
            return;
        }
        String sql = sqlTemplateService.load("api_permission_upsert.sql");
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (BootstrapControllerApiScanner.PermissionSeed permission : permissions) {
                ps.setString(1, permission.code());
                ps.setString(2, permission.name());
                ps.setString(3, permission.userScope().name());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void batchInsertApis(Connection connection, List<BootstrapControllerApiScanner.ApiSeed> apis,
            Map<String, String> existingSortOptions) throws SQLException {
        if (apis.isEmpty()) {
            return;
        }
        String sql = sqlTemplateService.load("api_insert.sql");
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (BootstrapControllerApiScanner.ApiSeed api : apis) {
                ps.setString(1, api.module());
                ps.setString(2, api.protocol().name());
                ps.setString(3, api.httpMethod().name());
                ps.setString(4, api.pathPattern());
                ps.setString(5, api.handlerClass());
                ps.setString(6, api.handlerMethod());
                ps.setBoolean(7, api.permissionDeclared());
                ps.setString(8, api.accessType().name());
                ps.setString(9, api.userType());
                ps.setBoolean(10, api.auditDeclared());
                ps.setString(11, api.auditResource());
                ps.setString(12, api.auditAction());
                ps.setString(13, api.auditDescription());
                ps.setString(14, existingSortOptions.getOrDefault(apiKey(api.module(), api.protocol().name(),
                        api.httpMethod().name(), api.pathPattern()), api.sortOptionsJson()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void batchInsertApiPermissions(Connection connection,
            List<BootstrapControllerApiScanner.ApiSeed> apis) throws SQLException {
        String sql = sqlTemplateService.load("api_permission_link_insert.sql");
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (BootstrapControllerApiScanner.ApiSeed api : apis) {
                for (String permissionCode : api.permissionCodes()) {
                    ps.setString(1, permissionCode);
                    ps.setString(2, api.module());
                    ps.setString(3, api.protocol().name());
                    ps.setString(4, api.httpMethod().name());
                    ps.setString(5, api.pathPattern());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    private void executeUpdate(Connection connection, String sql) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    private String apiKey(String module, String protocol, String httpMethod, String pathPattern) {
        return module + "|" + protocol + "|" + httpMethod + "|" + pathPattern;
    }
}

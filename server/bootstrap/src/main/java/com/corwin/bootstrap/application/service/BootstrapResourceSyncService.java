package com.corwin.bootstrap.application.service;

import com.corwin.bootstrap.application.BootstrapTaskKey;
import com.corwin.bootstrap.application.BootstrapTaskReport;
import com.corwin.system.resource.domain.model.PermissionUserScope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * @author Corwin 2026/5/5
 */
@Service
@RequiredArgsConstructor
public class BootstrapResourceSyncService {

    private final DataSource dataSource;
    private final BootstrapDefinitionResources definitionResources;
    private final BootstrapSqlTemplateService sqlTemplateService;
    private final BootstrapXmlValidationService xmlValidationService;
    private final BootstrapResourceDefinitionLoader resourceDefinitionLoader;
    private final BootstrapUserFeatureDefinitionLoader userFeatureDefinitionLoader;

    public BootstrapTaskReport run(boolean dryRun) {
        long startedAt = System.currentTimeMillis();
        xmlValidationService.validate(definitionResources.resourcesXml(), definitionResources.resourcesXsd());
        xmlValidationService.validate(definitionResources.userFeaturesXml(), definitionResources.userFeaturesXsd());

        List<BootstrapResourceDefinitionLoader.MenuSeed> menus = resourceDefinitionLoader.loadDefinitions(
                definitionResources.resourcesXml());
        BootstrapUserFeatureDefinitionLoader.UserFeatureDefinitionSeed userFeatures = userFeatureDefinitionLoader.loadDefinitions(
                definitionResources.userFeaturesXml());
        ResourceStats expectedStats = summarize(menus, userFeatures);

        ResourceStats stats = dryRun ? validateOnly(menus, userFeatures,
                expectedStats) : BootstrapJdbcTransactionSupport.execute(dataSource,
                connection -> rebuild(connection, menus, userFeatures));

        String message = "menus=" + stats.menuCount
                + "; functions=" + stats.functionCount
                + "; functionPermissions=" + stats.functionPermissionCount
                + "; applications=" + stats.applicationCount
                + "; features=" + stats.featureCount
                + "; featurePermissions=" + stats.featurePermissionCount
                + "; strategy=resource-rebuild+userfeature-rebuild";
        return new BootstrapTaskReport(BootstrapTaskKey.RESOURCE_SYNC, dryRun, true,
                System.currentTimeMillis() - startedAt, message);
    }

    private ResourceStats validateOnly(List<BootstrapResourceDefinitionLoader.MenuSeed> menus,
            BootstrapUserFeatureDefinitionLoader.UserFeatureDefinitionSeed userFeatures, ResourceStats expectedStats) {
        return BootstrapJdbcTransactionSupport.execute(dataSource, connection -> {
            Map<String, PermissionRef> permissionByCode = loadPermissionByCode(connection,
                    sqlTemplateService.load("resource_select_permission_code_id.sql"));
            validatePermissionReferences(permissionByCode, menus, userFeatures);
            return expectedStats;
        });
    }

    private ResourceStats rebuild(Connection connection, List<BootstrapResourceDefinitionLoader.MenuSeed> menus,
            BootstrapUserFeatureDefinitionLoader.UserFeatureDefinitionSeed userFeatures) throws SQLException {
        Map<String, PermissionRef> permissionByCode = loadPermissionByCode(connection,
                sqlTemplateService.load("resource_select_permission_code_id.sql"));
        validatePermissionReferences(permissionByCode, menus, userFeatures);

        executeDelete(connection, "resource_delete_role_functions.sql");
        executeDelete(connection, "resource_delete_role_menus.sql");
        executeDelete(connection, "resource_delete_function_permissions.sql");
        executeDelete(connection, "resource_delete_menu_functions.sql");
        executeDelete(connection, "resource_delete_functions.sql");
        executeDelete(connection, "resource_delete_menus.sql");

        ResourceStats stats = new ResourceStats();
        for (BootstrapResourceDefinitionLoader.MenuSeed menu : menus) {
            insertMenuTree(connection, menu, null, permissionByCode, stats);
        }
        upsertUserFeatures(connection, userFeatures, permissionByCode, stats);
        return stats;
    }

    private void validatePermissionReferences(Map<String, PermissionRef> permissionByCode,
            List<BootstrapResourceDefinitionLoader.MenuSeed> menus,
            BootstrapUserFeatureDefinitionLoader.UserFeatureDefinitionSeed userFeatures) {
        LinkedHashSet<String> internalPermissionCodes = new LinkedHashSet<>();
        for (BootstrapResourceDefinitionLoader.MenuSeed menu : menus) {
            collectInternalPermissionCodes(menu, internalPermissionCodes);
        }
        LinkedHashSet<String> externalPermissionCodes = new LinkedHashSet<>();
        for (BootstrapUserFeatureDefinitionLoader.ApplicationSeed application : userFeatures.applications()) {
            for (BootstrapUserFeatureDefinitionLoader.FeatureSeed feature : application.features()) {
                externalPermissionCodes.addAll(feature.permissionCodes());
            }
        }

        for (String permissionCode : internalPermissionCodes) {
            PermissionRef permission = permissionByCode.get(permissionCode);
            if (permission == null) {
                throw new IllegalStateException("resource permission missing in sys_permission: " + permissionCode);
            }
            if (permission.userScope() == PermissionUserScope.EXTERNAL) {
                throw new IllegalStateException("resource permission must not be EXTERNAL-only: " + permissionCode);
            }
        }
        for (String permissionCode : externalPermissionCodes) {
            PermissionRef permission = permissionByCode.get(permissionCode);
            if (permission == null) {
                throw new IllegalStateException(
                        "user feature permission missing in sys_permission: " + permissionCode);
            }
            if (permission.userScope() == PermissionUserScope.INTERNAL) {
                throw new IllegalStateException(
                        "user feature permission must not be INTERNAL-only: " + permissionCode);
            }
        }
    }

    private void collectInternalPermissionCodes(BootstrapResourceDefinitionLoader.MenuSeed menu,
            Set<String> collector) {
        for (BootstrapResourceDefinitionLoader.FunctionSeed function : menu.functions()) {
            collectFunctionPermissionCodes(function, collector);
        }
        for (BootstrapResourceDefinitionLoader.MenuSeed child : menu.children()) {
            collectInternalPermissionCodes(child, collector);
        }
    }

    private void collectFunctionPermissionCodes(BootstrapResourceDefinitionLoader.FunctionSeed function,
            Set<String> collector) {
        collector.addAll(function.permissionCodes());
        for (BootstrapResourceDefinitionLoader.FunctionSeed child : function.children()) {
            collectFunctionPermissionCodes(child, collector);
        }
    }

    private void insertMenuTree(Connection connection, BootstrapResourceDefinitionLoader.MenuSeed menu,
            Long parentMenuId, Map<String, PermissionRef> permissionByCode, ResourceStats stats) throws SQLException {
        Long menuId = insertMenu(connection, menu, parentMenuId);
        stats.menuCount++;
        for (BootstrapResourceDefinitionLoader.FunctionSeed function : menu.functions()) {
            insertFunctionTree(connection, menuId, null, function, permissionByCode, stats);
        }
        for (BootstrapResourceDefinitionLoader.MenuSeed child : menu.children()) {
            insertMenuTree(connection, child, menuId, permissionByCode, stats);
        }
    }

    private void insertFunctionTree(Connection connection, Long menuId, Long parentMenuFunctionId,
            BootstrapResourceDefinitionLoader.FunctionSeed function, Map<String, PermissionRef> permissionByCode,
            ResourceStats stats) throws SQLException {
        Long functionId = insertFunction(connection, function);
        stats.functionCount++;
        Long menuFunctionId = insertMenuFunction(connection, menuId, functionId, parentMenuFunctionId, function);
        for (String permissionCode : function.permissionCodes()) {
            PermissionRef permission = permissionByCode.get(permissionCode);
            insertFunctionPermission(connection, functionId, permission.id());
            stats.functionPermissionCount++;
        }
        for (BootstrapResourceDefinitionLoader.FunctionSeed child : function.children()) {
            insertFunctionTree(connection, menuId, menuFunctionId, child, permissionByCode, stats);
        }
    }

    private void upsertUserFeatures(Connection connection,
            BootstrapUserFeatureDefinitionLoader.UserFeatureDefinitionSeed seed,
            Map<String, PermissionRef> permissionByCode, ResourceStats stats) throws SQLException {
        clearUserFeatureData(connection);

        for (BootstrapUserFeatureDefinitionLoader.ApplicationSeed application : seed.applications()) {
            Long applicationId = upsertApplication(connection, application);
            stats.applicationCount++;

            for (BootstrapUserFeatureDefinitionLoader.FeatureSeed feature : application.features()) {
                Long featureId = upsertFeature(connection, applicationId, feature);
                stats.featureCount++;
                for (String permissionCode : feature.permissionCodes()) {
                    PermissionRef permission = permissionByCode.get(permissionCode);
                    insertFeaturePermissionBinding(connection, applicationId, featureId, permission.id());
                    stats.featurePermissionCount++;
                }
            }
        }
    }

    private void clearUserFeatureData(Connection connection) throws SQLException {
        executeSql(connection, "delete from sys_user_feature_override");
        executeSql(connection, "delete from sys_user_application_override");
        executeSql(connection, "delete from sys_user_package_feature_access");
        executeSql(connection, "delete from sys_user_package_application_access");
        executeSql(connection, "delete from sys_user_application_package_member");
        executeSql(connection, "delete from sys_user_application_package");
        executeSql(connection, "delete from sys_product_feature_permission_binding");
        executeSql(connection, "delete from sys_product_feature");
        executeSql(connection, "delete from sys_product_application");
    }

    private Long insertMenu(Connection connection, BootstrapResourceDefinitionLoader.MenuSeed menu,
            Long parentMenuId) throws SQLException {
        String sql = sqlTemplateService.load("resource_insert_menu.sql");
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, menu.code());
            bindNullableLong(ps, 2, parentMenuId);
            ps.setString(3, menu.name());
            ps.setString(4, menu.path());
            ps.setString(5, menu.component());
            ps.setString(6, menu.icon());
            ps.setString(7, menu.menuType().name());
            ps.setInt(8, menu.sortNo());
            ps.setBoolean(9, menu.visible());
            ps.setString(10, menu.remark());
            ps.executeUpdate();
            return readGeneratedKey(ps, "menu", menu.code());
        }
    }

    private Long insertFunction(Connection connection, BootstrapResourceDefinitionLoader.FunctionSeed function) throws
            SQLException {
        String sql = sqlTemplateService.load("resource_insert_function.sql");
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, function.code());
            ps.setString(2, function.name());
            ps.setString(3, function.functionType().name());
            ps.setString(4, function.description());
            ps.executeUpdate();
            return readGeneratedKey(ps, "function", function.code());
        }
    }

    private Long insertMenuFunction(Connection connection, Long menuId, Long functionId, Long parentMenuFunctionId,
            BootstrapResourceDefinitionLoader.FunctionSeed function) throws SQLException {
        String sql = sqlTemplateService.load("resource_insert_menu_function.sql");
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, menuId);
            ps.setLong(2, functionId);
            bindNullableLong(ps, 3, parentMenuFunctionId);
            ps.setInt(4, function.sortNo());
            ps.setBoolean(5, function.visible());
            ps.setBoolean(6, function.defaultEntry());
            ps.executeUpdate();
            return readGeneratedKey(ps, "menuFunction", function.code());
        }
    }

    private void insertFunctionPermission(Connection connection, Long functionId, Long permissionId) throws
            SQLException {
        String sql = sqlTemplateService.load("resource_insert_function_permission.sql");
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, functionId);
            ps.setLong(2, permissionId);
            ps.executeUpdate();
        }
    }

    private Long upsertApplication(Connection connection, BootstrapUserFeatureDefinitionLoader.ApplicationSeed seed) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sqlTemplateService.load("user_feature_update_application_by_code.sql"))) {
            ps.setString(1, seed.name());
            ps.setString(2, seed.description());
            ps.setString(3, seed.icon());
            ps.setString(4, seed.routePath());
            ps.setString(5, seed.componentPath());
            ps.setBoolean(6, seed.enabled());
            ps.setBoolean(7, true);
            ps.setInt(8, seed.sortNo());
            ps.setLong(9, 0L);
            ps.setTimestamp(10, new Timestamp(System.currentTimeMillis()));
            ps.setString(11, seed.code());
            if (ps.executeUpdate() == 0) {
                try (PreparedStatement insertPs = connection.prepareStatement(sqlTemplateService.load("user_feature_insert_application.sql"))) {
                    insertPs.setString(1, seed.code());
                    insertPs.setString(2, seed.name());
                    insertPs.setString(3, seed.description());
                    insertPs.setString(4, seed.icon());
                    insertPs.setString(5, seed.routePath());
                    insertPs.setString(6, seed.componentPath());
                    insertPs.setBoolean(7, seed.enabled());
                    insertPs.setBoolean(8, true);
                    insertPs.setInt(9, seed.sortNo());
                    insertPs.setLong(10, 0L);
                    insertPs.setTimestamp(11, new Timestamp(System.currentTimeMillis()));
                    insertPs.setLong(12, 0L);
                    insertPs.setTimestamp(13, new Timestamp(System.currentTimeMillis()));
                    insertPs.executeUpdate();
                }
            }
        }
        return selectIdByCode(connection, "user_feature_select_application_id_by_code.sql", seed.code(), "application");
    }

    private Long upsertFeature(Connection connection, Long applicationId,
            BootstrapUserFeatureDefinitionLoader.FeatureSeed seed) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sqlTemplateService.load("user_feature_update_feature_by_application_and_code.sql"))) {
            ps.setString(1, seed.name());
            ps.setString(2, seed.description());
            ps.setBoolean(3, seed.enabled());
            ps.setBoolean(4, true);
            ps.setInt(5, seed.sortNo());
            ps.setLong(6, 0L);
            ps.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
            ps.setLong(8, applicationId);
            ps.setString(9, seed.code());
            if (ps.executeUpdate() == 0) {
                try (PreparedStatement insertPs = connection.prepareStatement(sqlTemplateService.load("user_feature_insert_feature.sql"))) {
                    insertPs.setLong(1, applicationId);
                    insertPs.setString(2, seed.code());
                    insertPs.setString(3, seed.name());
                    insertPs.setString(4, "OPERATION");
                    insertPs.setString(5, seed.description());
                    insertPs.setBoolean(6, seed.enabled());
                    insertPs.setBoolean(7, true);
                    insertPs.setInt(8, seed.sortNo());
                    insertPs.setLong(9, 0L);
                    insertPs.setTimestamp(10, new Timestamp(System.currentTimeMillis()));
                    insertPs.setLong(11, 0L);
                    insertPs.setTimestamp(12, new Timestamp(System.currentTimeMillis()));
                    insertPs.executeUpdate();
                }
            }
        }
        return selectFeatureId(connection, applicationId, seed.code());
    }

    private Long selectIdByCode(Connection connection, String sqlName, String code, String type) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sqlTemplateService.load(sqlName))) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }
        throw new IllegalStateException("read " + type + " id failed: " + code);
    }

    private Long selectFeatureId(Connection connection, Long applicationId, String code) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sqlTemplateService.load("user_feature_select_feature_id_by_application_and_code.sql"))) {
            ps.setLong(1, applicationId);
            ps.setString(2, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }
        throw new IllegalStateException("read feature id failed: " + applicationId + ":" + code);
    }

    private void insertFeaturePermissionBinding(Connection connection, Long applicationId, Long featureId, Long permissionId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sqlTemplateService.load("user_feature_insert_feature_permission_binding.sql"))) {
            ps.setLong(1, applicationId);
            ps.setLong(2, featureId);
            ps.setLong(3, permissionId);
            ps.setBoolean(4, true);
            ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
        }
    }

    private Map<String, PermissionRef> loadPermissionByCode(Connection connection, String sql) throws SQLException {
        LinkedHashMap<String, PermissionRef> result = new LinkedHashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("code"),
                        new PermissionRef(rs.getLong("id"), PermissionUserScope.valueOf(rs.getString("user_scope"))));
            }
        }
        return result;
    }

    private void executeDelete(Connection connection, String sqlName) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sqlTemplateService.load(sqlName))) {
            ps.executeUpdate();
        }
    }

    private void executeSql(Connection connection, String sql) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    private Long readGeneratedKey(PreparedStatement ps, String type, String code) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
        }
        throw new IllegalStateException("Read generated key failed for " + type + ": " + code);
    }

    private void bindNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.BIGINT);
            return;
        }
        ps.setLong(index, value);
    }

    private ResourceStats summarize(List<BootstrapResourceDefinitionLoader.MenuSeed> menus,
            BootstrapUserFeatureDefinitionLoader.UserFeatureDefinitionSeed userFeatures) {
        ResourceStats stats = new ResourceStats();
        for (BootstrapResourceDefinitionLoader.MenuSeed menu : menus) {
            summarizeMenu(menu, stats);
        }
        stats.applicationCount = userFeatures.applications().size();
        for (BootstrapUserFeatureDefinitionLoader.ApplicationSeed application : userFeatures.applications()) {
            stats.featureCount += application.features().size();
            for (BootstrapUserFeatureDefinitionLoader.FeatureSeed feature : application.features()) {
                stats.featurePermissionCount += feature.permissionCodes().size();
            }
        }
        return stats;
    }

    private void summarizeMenu(BootstrapResourceDefinitionLoader.MenuSeed menu, ResourceStats stats) {
        stats.menuCount++;
        for (BootstrapResourceDefinitionLoader.FunctionSeed function : menu.functions()) {
            summarizeFunction(function, stats);
        }
        for (BootstrapResourceDefinitionLoader.MenuSeed child : menu.children()) {
            summarizeMenu(child, stats);
        }
    }

    private void summarizeFunction(BootstrapResourceDefinitionLoader.FunctionSeed function, ResourceStats stats) {
        stats.functionCount++;
        stats.functionPermissionCount += function.permissionCodes().size();
        for (BootstrapResourceDefinitionLoader.FunctionSeed child : function.children()) {
            summarizeFunction(child, stats);
        }
    }

    private record PermissionRef(
            Long id,
            PermissionUserScope userScope
    ) {
    }

    private static final class ResourceStats {
        private int menuCount;
        private int functionCount;
        private int functionPermissionCount;
        private int applicationCount;
        private int featureCount;
        private int featurePermissionCount;
    }
}

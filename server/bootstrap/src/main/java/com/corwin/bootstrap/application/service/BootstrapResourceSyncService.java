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
    private final BootstrapNormalFeatureDefinitionLoader normalFeatureDefinitionLoader;

    public BootstrapTaskReport run(boolean dryRun) {
        long startedAt = System.currentTimeMillis();
        xmlValidationService.validate(definitionResources.resourcesXml(), definitionResources.resourcesXsd());
        xmlValidationService.validate(definitionResources.normalFeaturesXml(), definitionResources.normalFeaturesXsd());

        List<BootstrapResourceDefinitionLoader.MenuSeed> menus = resourceDefinitionLoader.loadDefinitions(
                definitionResources.resourcesXml());
        List<BootstrapNormalFeatureDefinitionLoader.FeatureSeed> normalFeatures = normalFeatureDefinitionLoader.loadDefinitions(
                definitionResources.normalFeaturesXml());
        ResourceStats expectedStats = summarize(menus, normalFeatures);

        ResourceStats stats = dryRun ? validateOnly(menus, normalFeatures,
                expectedStats) : BootstrapJdbcTransactionSupport.execute(dataSource,
                connection -> rebuild(connection, menus, normalFeatures));

        String message = "menus=" + stats.menuCount + "; functions=" + stats.functionCount + "; functionPermissions=" + stats.functionPermissionCount + "; normalFeatures=" + stats.normalFeatureCount + "; normalFeaturePermissions=" + stats.normalFeaturePermissionCount + "; strategy=delete-all-and-rebuild";
        return new BootstrapTaskReport(BootstrapTaskKey.RESOURCE_SYNC, dryRun, true,
                System.currentTimeMillis() - startedAt, message);
    }

    private ResourceStats validateOnly(List<BootstrapResourceDefinitionLoader.MenuSeed> menus,
            List<BootstrapNormalFeatureDefinitionLoader.FeatureSeed> normalFeatures, ResourceStats expectedStats) {
        return BootstrapJdbcTransactionSupport.execute(dataSource, connection -> {
            Map<String, PermissionRef> permissionByCode = loadPermissionByCode(connection,
                    sqlTemplateService.load("resource_select_permission_code_id.sql"));
            validatePermissionReferences(permissionByCode, menus, normalFeatures);
            return expectedStats;
        });
    }

    private ResourceStats rebuild(Connection connection, List<BootstrapResourceDefinitionLoader.MenuSeed> menus,
            List<BootstrapNormalFeatureDefinitionLoader.FeatureSeed> normalFeatures) throws SQLException {
        Map<String, PermissionRef> permissionByCode = loadPermissionByCode(connection,
                sqlTemplateService.load("resource_select_permission_code_id.sql"));
        validatePermissionReferences(permissionByCode, menus, normalFeatures);

        executeDelete(connection, "resource_delete_role_functions.sql");
        executeDelete(connection, "resource_delete_role_menus.sql");
        executeDelete(connection, "resource_delete_function_permissions.sql");
        executeDelete(connection, "resource_delete_menu_functions.sql");
        executeDelete(connection, "resource_delete_functions.sql");
        executeDelete(connection, "resource_delete_menus.sql");

        executeDelete(connection, "normal_feature_delete_user_overrides.sql");
        executeDelete(connection, "normal_feature_delete_group_grants.sql");
        executeDelete(connection, "normal_feature_delete_group_members.sql");
        executeDelete(connection, "normal_feature_delete_groups.sql");
        executeDelete(connection, "normal_feature_delete_feature_permissions.sql");
        executeDelete(connection, "normal_feature_delete_features.sql");

        ResourceStats stats = new ResourceStats();
        for (BootstrapResourceDefinitionLoader.MenuSeed menu : menus) {
            insertMenuTree(connection, menu, null, permissionByCode, stats);
        }
        insertNormalFeatures(connection, normalFeatures, permissionByCode, stats);
        return stats;
    }

    private void validatePermissionReferences(Map<String, PermissionRef> permissionByCode,
            List<BootstrapResourceDefinitionLoader.MenuSeed> menus,
            List<BootstrapNormalFeatureDefinitionLoader.FeatureSeed> normalFeatures) {
        LinkedHashSet<String> internalPermissionCodes = new LinkedHashSet<>();
        for (BootstrapResourceDefinitionLoader.MenuSeed menu : menus) {
            collectInternalPermissionCodes(menu, internalPermissionCodes);
        }
        LinkedHashSet<String> normalPermissionCodes = new LinkedHashSet<>();
        for (BootstrapNormalFeatureDefinitionLoader.FeatureSeed feature : normalFeatures) {
            normalPermissionCodes.addAll(feature.permissionCodes());
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
        for (String permissionCode : normalPermissionCodes) {
            PermissionRef permission = permissionByCode.get(permissionCode);
            if (permission == null) {
                throw new IllegalStateException(
                        "normal feature permission missing in sys_permission: " + permissionCode);
            }
            if (permission.userScope() == PermissionUserScope.INTERNAL) {
                throw new IllegalStateException(
                        "normal feature permission must not be INTERNAL-only: " + permissionCode);
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

    private void insertNormalFeatures(Connection connection,
            List<BootstrapNormalFeatureDefinitionLoader.FeatureSeed> normalFeatures,
            Map<String, PermissionRef> permissionByCode, ResourceStats stats) throws SQLException {
        Map<String, Long> featureIdByCode = new LinkedHashMap<>();
        for (BootstrapNormalFeatureDefinitionLoader.FeatureSeed feature : normalFeatures) {
            Long featureId = insertNormalFeature(connection, feature);
            featureIdByCode.put(feature.code(), featureId);
            stats.normalFeatureCount++;
        }
        for (BootstrapNormalFeatureDefinitionLoader.FeatureSeed feature : normalFeatures) {
            Long featureId = featureIdByCode.get(feature.code());
            for (String permissionCode : feature.permissionCodes()) {
                PermissionRef permission = permissionByCode.get(permissionCode);
                insertNormalFeaturePermission(connection, featureId, permission.id());
                stats.normalFeaturePermissionCount++;
            }
        }
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

    private Long insertNormalFeature(Connection connection,
            BootstrapNormalFeatureDefinitionLoader.FeatureSeed feature) throws SQLException {
        String sql = sqlTemplateService.load("normal_feature_insert_feature.sql");
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, feature.code());
            ps.setString(2, feature.name());
            ps.setString(3, feature.description());
            ps.setBoolean(4, feature.enabled());
            ps.executeUpdate();
            return readGeneratedKey(ps, "normalFeature", feature.code());
        }
    }

    private void insertNormalFeaturePermission(Connection connection, Long featureId, Long permissionId) throws
            SQLException {
        String sql = sqlTemplateService.load("normal_feature_insert_feature_permission.sql");
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, featureId);
            ps.setLong(2, permissionId);
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
            List<BootstrapNormalFeatureDefinitionLoader.FeatureSeed> normalFeatures) {
        ResourceStats stats = new ResourceStats();
        for (BootstrapResourceDefinitionLoader.MenuSeed menu : menus) {
            summarizeMenu(menu, stats);
        }
        stats.normalFeatureCount = normalFeatures.size();
        for (BootstrapNormalFeatureDefinitionLoader.FeatureSeed feature : normalFeatures) {
            stats.normalFeaturePermissionCount += feature.permissionCodes().size();
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
        private int normalFeatureCount;
        private int normalFeaturePermissionCount;
    }
}

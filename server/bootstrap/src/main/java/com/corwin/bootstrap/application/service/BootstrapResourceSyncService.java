package com.corwin.bootstrap.application.service;

import com.corwin.bootstrap.application.BootstrapTaskKey;
import com.corwin.bootstrap.application.BootstrapTaskReport;
import com.corwin.framework.constant.UserType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Corwin 2026/6/29
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

        List<BootstrapResourceDefinitionLoader.ResourceSeed> resources = resourceDefinitionLoader.loadDefinitions(
                definitionResources.resourcesXml());
        BootstrapUserFeatureDefinitionLoader.UserFeatureDefinitionSeed userFeatures = userFeatureDefinitionLoader.loadDefinitions(
                definitionResources.userFeaturesXml());
        ResourceStats expectedStats = summarize(resources, userFeatures);

        ResourceStats stats = dryRun ? validateOnly(resources, userFeatures, expectedStats)
                : BootstrapJdbcTransactionSupport.execute(dataSource,
                        connection -> rebuild(connection, resources, userFeatures));

        String message = "resources=" + stats.resourceCount
                + "; resourcePermissions=" + stats.resourcePermissionCount
                + "; applications=" + stats.applicationCount
                + "; features=" + stats.featureCount
                + "; featurePermissions=" + stats.featurePermissionCount
                + "; strategy=resource-rebuild+userfeature-rebuild";
        return new BootstrapTaskReport(BootstrapTaskKey.RESOURCE_SYNC, dryRun, true,
                System.currentTimeMillis() - startedAt, message);
    }

    private ResourceStats validateOnly(List<BootstrapResourceDefinitionLoader.ResourceSeed> resources,
            BootstrapUserFeatureDefinitionLoader.UserFeatureDefinitionSeed userFeatures, ResourceStats expectedStats) {
        return BootstrapJdbcTransactionSupport.execute(dataSource, connection -> {
            Map<String, PermissionRef> permissionByCode = loadPermissionByCode(connection,
                    sqlTemplateService.load("resource_select_permission_code_id.sql"));
            validatePermissionReferences(permissionByCode, resources, userFeatures);
            return expectedStats;
        });
    }

    private ResourceStats rebuild(Connection connection, List<BootstrapResourceDefinitionLoader.ResourceSeed> resources,
            BootstrapUserFeatureDefinitionLoader.UserFeatureDefinitionSeed userFeatures) throws SQLException {
        Map<String, PermissionRef> permissionByCode = loadPermissionByCode(connection,
                sqlTemplateService.load("resource_select_permission_code_id.sql"));
        validatePermissionReferences(permissionByCode, resources, userFeatures);

        executeDelete(connection, "resource_delete_role_resources.sql");
        executeDelete(connection, "resource_delete_resource_permissions.sql");
        executeDelete(connection, "resource_delete_resources.sql");

        ResourceStats stats = new ResourceStats();
        for (BootstrapResourceDefinitionLoader.ResourceSeed resource : resources) {
            insertResourceTree(connection, null, resource, permissionByCode, stats);
        }
        upsertUserFeatures(connection, userFeatures, permissionByCode, stats);
        return stats;
    }

    private void validatePermissionReferences(Map<String, PermissionRef> permissionByCode,
            List<BootstrapResourceDefinitionLoader.ResourceSeed> resources,
            BootstrapUserFeatureDefinitionLoader.UserFeatureDefinitionSeed userFeatures) {
        LinkedHashSet<String> internalPermissionCodes = new LinkedHashSet<>();
        for (BootstrapResourceDefinitionLoader.ResourceSeed resource : resources) {
            collectInternalPermissionCodes(resource, internalPermissionCodes);
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
            if (permission.userScope() != UserType.INTERNAL) {
                throw new IllegalStateException("resource permission must be INTERNAL-only: " + permissionCode);
            }
        }
        for (String permissionCode : externalPermissionCodes) {
            PermissionRef permission = permissionByCode.get(permissionCode);
            if (permission == null) {
                throw new IllegalStateException("user feature permission missing in sys_permission: " + permissionCode);
            }
            if (permission.userScope() != UserType.EXTERNAL) {
                throw new IllegalStateException("user feature permission must be EXTERNAL-only: " + permissionCode);
            }
        }
    }

    private void collectInternalPermissionCodes(BootstrapResourceDefinitionLoader.ResourceSeed resource,
            Set<String> collector) {
        if (resource.resourceType().canBindPermission()) {
            collector.addAll(resource.permissionCodes());
        }
        for (BootstrapResourceDefinitionLoader.ResourceSeed child : resource.children()) {
            collectInternalPermissionCodes(child, collector);
        }
    }

    private void insertResourceTree(Connection connection, Long parentId,
            BootstrapResourceDefinitionLoader.ResourceSeed resource, Map<String, PermissionRef> permissionByCode,
            ResourceStats stats) throws SQLException {
        Long resourceId = insertResource(connection, parentId, resource);
        stats.resourceCount++;
        if (resource.resourceType().canBindPermission()) {
            for (String permissionCode : resource.permissionCodes()) {
                PermissionRef permission = permissionByCode.get(permissionCode);
                insertResourcePermission(connection, resourceId, permission.id());
                stats.resourcePermissionCount++;
            }
        }
        for (BootstrapResourceDefinitionLoader.ResourceSeed child : resource.children()) {
            insertResourceTree(connection, resourceId, child, permissionByCode, stats);
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

    private Long insertResource(Connection connection, Long parentId,
            BootstrapResourceDefinitionLoader.ResourceSeed resource) throws SQLException {
        String sql = sqlTemplateService.load("resource_insert_resource.sql");
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, resource.code());
            bindNullableLong(ps, 2, parentId);
            ps.setString(3, resource.name());
            ps.setString(4, resource.resourceType().name());
            ps.setString(5, resource.path());
            ps.setString(6, resource.component());
            ps.setString(7, resource.icon());
            ps.setInt(8, resource.sortNo());
            ps.setBoolean(9, resource.visible());
            ps.setBoolean(10, resource.enabled());
            ps.setBoolean(11, resource.defaultEntry());
            ps.setString(12, resource.remark());
            ps.executeUpdate();
            return readGeneratedKey(ps, "resource", resource.code());
        }
    }

    private void insertResourcePermission(Connection connection, Long resourceId, Long permissionId)
            throws SQLException {
        String sql = sqlTemplateService.load("resource_insert_resource_permission.sql");
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, resourceId);
            ps.setLong(2, permissionId);
            ps.executeUpdate();
        }
    }

    private Long upsertApplication(Connection connection, BootstrapUserFeatureDefinitionLoader.ApplicationSeed seed)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                sqlTemplateService.load("user_feature_update_application_by_code.sql"))) {
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
                try (PreparedStatement insertPs = connection.prepareStatement(
                        sqlTemplateService.load("user_feature_insert_application.sql"))) {
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
        return selectIdByCode(connection, "user_feature_select_application_id_by_code.sql", seed.code(),
                "application");
    }

    private Long upsertFeature(Connection connection, Long applicationId,
            BootstrapUserFeatureDefinitionLoader.FeatureSeed seed) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                sqlTemplateService.load("user_feature_update_feature_by_application_and_code.sql"))) {
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
                try (PreparedStatement insertPs = connection.prepareStatement(
                        sqlTemplateService.load("user_feature_insert_feature.sql"))) {
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
        try (PreparedStatement ps = connection.prepareStatement(
                sqlTemplateService.load("user_feature_select_feature_id_by_application_and_code.sql"))) {
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

    private void insertFeaturePermissionBinding(Connection connection, Long applicationId, Long featureId,
            Long permissionId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                sqlTemplateService.load("user_feature_insert_feature_permission_binding.sql"))) {
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
                        new PermissionRef(rs.getLong("id"), UserType.valueOf(rs.getString("user_scope"))));
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

    private ResourceStats summarize(List<BootstrapResourceDefinitionLoader.ResourceSeed> resources,
            BootstrapUserFeatureDefinitionLoader.UserFeatureDefinitionSeed userFeatures) {
        ResourceStats stats = new ResourceStats();
        for (BootstrapResourceDefinitionLoader.ResourceSeed resource : resources) {
            summarizeResource(resource, stats);
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

    private void summarizeResource(BootstrapResourceDefinitionLoader.ResourceSeed resource, ResourceStats stats) {
        stats.resourceCount++;
        if (resource.resourceType().canBindPermission()) {
            stats.resourcePermissionCount += resource.permissionCodes().size();
        }
        for (BootstrapResourceDefinitionLoader.ResourceSeed child : resource.children()) {
            summarizeResource(child, stats);
        }
    }

    private record PermissionRef(
            Long id,
            UserType userScope
    ) {
    }

    private static final class ResourceStats {
        private int resourceCount;
        private int resourcePermissionCount;
        private int applicationCount;
        private int featureCount;
        private int featurePermissionCount;
    }
}

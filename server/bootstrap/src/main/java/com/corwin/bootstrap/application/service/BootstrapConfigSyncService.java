package com.corwin.bootstrap.application.service;

import com.corwin.bootstrap.application.BootstrapTaskKey;
import com.corwin.bootstrap.application.BootstrapTaskReport;
import com.corwin.framework.config.ConfigDefinitionCatalog;
import com.corwin.framework.config.ConfigDefinitionDescriptor;
import com.corwin.framework.config.ConfigScope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * @author Corwin 2026/5/5
 */
@Service
@RequiredArgsConstructor
public class BootstrapConfigSyncService {

    private final DataSource dataSource;
    private final BootstrapSqlTemplateService sqlTemplateService;

    public BootstrapTaskReport run(boolean dryRun) {
        long startedAt = System.currentTimeMillis();
        List<ConfigDefinitionDescriptor> definitions = ConfigDefinitionCatalog.loadAll();
        Map<String, String> defaultValues = BootstrapConfigDefaultValues.loadAll();
        List<ConfigSeed> seeds = buildSeeds(definitions, defaultValues);

        if (seeds.isEmpty()) {
            return new BootstrapTaskReport(BootstrapTaskKey.CONFIG_SYNC, dryRun, true,
                    System.currentTimeMillis() - startedAt, "No config definition found");
        }

        Set<String> existingCodes = BootstrapJdbcTransactionSupport.execute(dataSource,
                connection -> loadExistingCodes(connection, sqlTemplateService.load("config_select_codes.sql")));
        List<ConfigSeed> createdSeeds = seeds.stream().filter(seed -> !existingCodes.contains(seed.definition().code()))
                .toList();
        List<String> unchangedCodes = seeds.stream().map(ConfigSeed::definition).map(ConfigDefinitionDescriptor::code)
                .filter(existingCodes::contains).toList();
        List<String> orphanCodes = existingCodes.stream()
                .filter(code -> seeds.stream().noneMatch(item -> item.definition().code().equals(code))).toList();

        if (!dryRun) {
            BootstrapJdbcTransactionSupport.executeWithoutResult(dataSource, connection -> {
                batchInsertMissing(connection, createdSeeds);
                markExpired(connection, orphanCodes);
            });
        }

        String message = "total=" + seeds.size() + "; created=" + createdSeeds.size() + "; unchanged=" + unchangedCodes.size() + "; expired=" + orphanCodes.size() + "; framework=" + countByScope(
                seeds, ConfigScope.FRAMEWORK) + "; system=" + countByScope(seeds,
                ConfigScope.SYSTEM) + "; business=" + countByScope(seeds, ConfigScope.BUSINESS);

        return new BootstrapTaskReport(BootstrapTaskKey.CONFIG_SYNC, dryRun, true,
                System.currentTimeMillis() - startedAt, message);
    }

    private Set<String> loadExistingCodes(Connection connection, String sql) throws SQLException {
        Set<String> result = new LinkedHashSet<>();
        try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(rs.getString(1));
            }
        }
        return result;
    }

    private void batchInsertMissing(Connection connection, List<ConfigSeed> seeds) throws SQLException {
        if (seeds.isEmpty()) {
            return;
        }
        String sql = sqlTemplateService.load("config_upsert.sql");
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (ConfigSeed seed : seeds) {
                bindInsertParameters(ps, seed);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void markExpired(Connection connection, List<String> orphanCodes) throws SQLException {
        if (orphanCodes.isEmpty()) {
            return;
        }
        String sql = sqlTemplateService.load("config_disable_missing.sql")
                .replace("${codePlaceholders}", String.join(", ", Collections.nCopies(orphanCodes.size(), "?")));
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < orphanCodes.size(); i++) {
                ps.setString(i + 1, orphanCodes.get(i));
            }
            ps.executeUpdate();
        }
    }

    private void bindInsertParameters(PreparedStatement ps, ConfigSeed seed) throws SQLException {
        ConfigDefinitionDescriptor definition = seed.definition();
        ps.setString(1, definition.code());
        ps.setString(2, seed.defaultValue());
        ps.setString(3, definition.valueType().name());
        ps.setString(4, definition.description());
        ps.setString(5, definition.level().name());
        ps.setBoolean(6, false);
    }

    private List<ConfigSeed> buildSeeds(List<ConfigDefinitionDescriptor> definitions,
            Map<String, String> defaultValues) {
        Map<String, String> unconsumedDefaultValues = new LinkedHashMap<>(defaultValues);
        List<String> missingCodes = new ArrayList<>();
        List<ConfigSeed> seeds = new ArrayList<>();
        for (ConfigDefinitionDescriptor definition : definitions) {
            String defaultValue = unconsumedDefaultValues.remove(definition.code());
            if (defaultValue == null) {
                missingCodes.add(definition.code());
                continue;
            }
            seeds.add(new ConfigSeed(definition, defaultValue));
        }

        if (!missingCodes.isEmpty()) {
            throw new IllegalStateException("Missing bootstrap default values for config definitions: " + missingCodes);
        }
        if (!unconsumedDefaultValues.isEmpty()) {
            throw new IllegalStateException(
                    "Bootstrap default values found for undefined config codes: " + unconsumedDefaultValues.keySet());
        }
        return seeds;
    }

    private long countByScope(List<ConfigSeed> seeds, ConfigScope scope) {
        return seeds.stream().filter(item -> item.definition().scope() == scope).count();
    }

    private record ConfigSeed(
            ConfigDefinitionDescriptor definition,
            String defaultValue
    ) {
    }

}

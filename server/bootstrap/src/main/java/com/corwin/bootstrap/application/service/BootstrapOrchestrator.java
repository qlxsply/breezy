package com.corwin.bootstrap.application.service;

import com.corwin.bootstrap.application.BootstrapTaskKey;
import com.corwin.bootstrap.application.BootstrapTaskReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Corwin 2026/5/5
 */
@Service
@RequiredArgsConstructor
public class BootstrapOrchestrator {

    private final BootstrapSchemaSyncService schemaSyncService;
    private final BootstrapConfigSyncService configSyncService;
    private final BootstrapDictionarySyncService dictionarySyncService;
    private final BootstrapApiSyncService apiSyncService;
    private final BootstrapResourceSyncService resourceSyncService;
    private final BootstrapDefaultUserSyncService defaultUserSyncService;
    private final BootstrapSystemFileSyncService systemFileSyncService;

    public List<BootstrapTaskReport> runAll(boolean dryRun) {
        List<BootstrapTaskReport> reports = new ArrayList<>();
        BootstrapTaskReport schemaReport = schemaSyncService.run(dryRun);
        reports.add(schemaReport);
        if (!schemaReport.success()) {
            return reports;
        }
        BootstrapTaskReport configReport = configSyncService.run(dryRun);
        reports.add(configReport);
        if (!configReport.success()) {
            return reports;
        }
        BootstrapTaskReport dictReport = dictionarySyncService.run(dryRun);
        reports.add(dictReport);
        if (!dictReport.success()) {
            return reports;
        }
        BootstrapTaskReport apiReport = apiSyncService.run(dryRun);
        reports.add(apiReport);
        if (!apiReport.success()) {
            return reports;
        }
        BootstrapTaskReport resourceReport = resourceSyncService.run(dryRun);
        reports.add(resourceReport);
        if (!resourceReport.success()) {
            return reports;
        }
        BootstrapTaskReport userReport = defaultUserSyncService.run(dryRun);
        reports.add(userReport);
        if (!userReport.success()) {
            return reports;
        }
        reports.add(systemFileSyncService.run(dryRun));
        return reports;
    }

    public BootstrapTaskReport runTask(BootstrapTaskKey task, boolean dryRun) {
        return switch (task) {
            case SCHEMA_SYNC -> schemaSyncService.run(dryRun);
            case CONFIG_SYNC -> configSyncService.run(dryRun);
            case DICTIONARY_SYNC -> dictionarySyncService.run(dryRun);
            case API_SYNC -> apiSyncService.run(dryRun);
            case RESOURCE_SYNC -> resourceSyncService.run(dryRun);
            case DEFAULT_USER_SYNC -> defaultUserSyncService.run(dryRun);
            case SYSTEM_FILE_SYNC -> systemFileSyncService.run(dryRun);
        };
    }
}

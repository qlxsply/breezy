package com.corwin.bootstrap.application.service;

import com.corwin.bootstrap.application.BootstrapTaskKey;
import com.corwin.bootstrap.application.BootstrapTaskReport;
import com.corwin.system.file.published.FilePurpose;
import com.corwin.system.file.published.OwnerType;
import com.corwin.system.user.domain.model.DefaultUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * @author Corwin 2026/5/6
 */
@Service
@RequiredArgsConstructor
public class BootstrapSystemFileSyncService {

    private final DataSource dataSource;
    private final BootstrapSqlTemplateService sqlTemplateService;

    public BootstrapTaskReport run(boolean dryRun) {
        long startedAt = System.currentTimeMillis();
        List<FolderSeed> seeds = buildSeeds();
        FolderSyncSummary summary = dryRun ? BootstrapJdbcTransactionSupport.execute(dataSource,
                connection -> inspect(connection, seeds)) : BootstrapJdbcTransactionSupport.execute(dataSource,
                connection -> synchronize(connection, seeds));

        String message = "total=" + seeds.size() + "; created=" + summary.created() + "; skipped=" + summary.skipped() + "; ownerType=" + OwnerType.APPLICATION.name() + "; ownerId=" + DefaultUser.SYSTEM.account();
        return new BootstrapTaskReport(BootstrapTaskKey.SYSTEM_FILE_SYNC, dryRun, true,
                System.currentTimeMillis() - startedAt, message);
    }

    private FolderSyncSummary inspect(Connection connection, List<FolderSeed> seeds) throws SQLException {
        Set<String> existingFolderNames = loadExistingFolderNames(connection);
        int created = 0;
        int skipped = 0;
        for (FolderSeed seed : seeds) {
            if (existingFolderNames.contains(seed.folderName())) {
                skipped++;
                continue;
            }
            created++;
        }
        return new FolderSyncSummary(created, skipped);
    }

    private FolderSyncSummary synchronize(Connection connection, List<FolderSeed> seeds) throws SQLException {
        Set<String> existingFolderNames = loadExistingFolderNames(connection);
        int created = 0;
        int skipped = 0;
        for (FolderSeed seed : seeds) {
            if (existingFolderNames.contains(seed.folderName())) {
                skipped++;
                continue;
            }
            insertFolder(connection, seed);
            existingFolderNames.add(seed.folderName());
            created++;
        }
        return new FolderSyncSummary(created, skipped);
    }

    private Set<String> loadExistingFolderNames(Connection connection) throws SQLException {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        String sql = sqlTemplateService.load("file_select_system_root_folders.sql");
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, OwnerType.APPLICATION.name());
            ps.setString(2, DefaultUser.SYSTEM.account());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString("folder_name"));
                }
            }
        }
        return result;
    }

    private void insertFolder(Connection connection, FolderSeed seed) throws SQLException {
        String sql = sqlTemplateService.load("file_insert_system_root_folder.sql");
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, seed.id());
            ps.setString(2, seed.ownerType());
            ps.setString(3, seed.ownerId());
            ps.setString(4, seed.folderName());
            ps.executeUpdate();
        }
    }

    private List<FolderSeed> buildSeeds() {
        List<FolderSeed> result = new ArrayList<>();
        String ownerType = OwnerType.APPLICATION.name();
        String ownerId = DefaultUser.SYSTEM.account();
        for (FilePurpose purpose : FilePurpose.values()) {
            result.add(new FolderSeed(UUID.randomUUID().toString(), ownerType, ownerId, purpose.name()));
        }
        return List.copyOf(result);
    }

    private record FolderSeed(
            String id,
            String ownerType,
            String ownerId,
            String folderName
    ) {
    }

    private record FolderSyncSummary(
            int created,
            int skipped
    ) {
    }
}

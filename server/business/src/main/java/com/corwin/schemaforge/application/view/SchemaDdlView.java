package com.corwin.schemaforge.application.view;

import java.time.Instant;

/**
 * @author Corwin 2026/2/24
 */
public record SchemaDdlView(
    String id,
    Long managedDatabaseId,
    String sourceSnapshotId,
    String targetSnapshotId,
    String name,
    String remark,
    String dbType,
    String dbVersion,
    String schemaName,
    String logicalFileId,
    String contentHash,
    Instant createdAt) {}

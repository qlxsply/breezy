package com.corwin.schemaforge.application.view;

import java.time.Instant;

/**
 * @author Corwin 2026/2/24
 */
public record SchemaSnapshotView(
    String id,
    Long managedDatabaseId,
    String name,
    String remark,
    String dbType,
    String dbVersion,
    String schemaName,
    String logicalFileId,
    String sqlLogicalFileId,
    String contentHash,
    Instant createdAt) {}

package com.corwin.schemaforge.interfaces.web.res;

import com.corwin.framework.json.JsonLongString;
import java.time.Instant;

/**
 * @author Corwin 2026/2/24
 */
public record SchemaSnapshotRes(
    String id,
    @JsonLongString Long managedDatabaseId,
    String name,
    String remark,
    String dbType,
    String dbVersion,
    String schemaName,
    String logicalFileId,
    String sqlLogicalFileId,
    String contentHash,
    Instant createdAt) {}

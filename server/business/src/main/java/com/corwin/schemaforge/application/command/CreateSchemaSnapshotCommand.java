package com.corwin.schemaforge.application.command;

import java.util.List;

/**
 * @author Corwin 2026/2/24
 */
public record CreateSchemaSnapshotCommand(
    Long managedDatabaseId, String name, String remark, List<Long> selectedObjectIds) {}

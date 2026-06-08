package com.corwin.schemaforge.application.command;

/**
 * @author Corwin 2026/2/24
 */
public record CreateSchemaDdlCommand(
        String sourceSnapshotId,
        String targetSnapshotId,
        String name,
        String remark
) {
}

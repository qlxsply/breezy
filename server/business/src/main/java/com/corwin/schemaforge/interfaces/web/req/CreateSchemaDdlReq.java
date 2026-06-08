package com.corwin.schemaforge.interfaces.web.req;

/**
 * @author Corwin 2026/2/24
 */
public record CreateSchemaDdlReq(
        String sourceSnapshotId,
        String targetSnapshotId,
        String name,
        String remark
) {
}

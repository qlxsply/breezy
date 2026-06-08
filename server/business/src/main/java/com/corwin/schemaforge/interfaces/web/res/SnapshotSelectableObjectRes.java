package com.corwin.schemaforge.interfaces.web.res;

import com.corwin.framework.json.JsonLongString;

/**
 * @author Corwin 2026/2/26
 */
public record SnapshotSelectableObjectRes(
        @JsonLongString Long id,
        String tableName,
        String tableSchema,
        String tableType,
        String alias
) {
}

package com.corwin.schemaforge.interfaces.web.req;

import com.corwin.framework.json.JsonLongString;
import java.util.List;

/**
 * @author Corwin 2026/2/24
 */
public record CreateSchemaSnapshotReq(
    @JsonLongString Long managedDatabaseId,
    String name,
    String remark,
    List<Long> selectedObjectIds) {}

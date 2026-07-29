package com.corwin.datasource.interfaces.web.req;

import com.corwin.framework.json.JsonLongString;

/**
 * @author Corwin 2026/7/29
 */
public record DatabaseSchemaListReq(
        @JsonLongString
        Long dataSourceId,
        Boolean unboundOnly,
        String sortBy,
        String sortDirection
) {
}

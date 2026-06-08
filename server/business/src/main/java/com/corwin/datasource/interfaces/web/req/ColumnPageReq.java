package com.corwin.datasource.interfaces.web.req;

import com.corwin.framework.json.JsonLongString;
import com.corwin.framework.web.request.PageRuleRequest;
import com.corwin.framework.web.request.SortRuleRequest;

/**
 * @author Corwin 2026/1/11
 */
public record ColumnPageReq(
        PageRuleRequest page,
        SortRuleRequest sort,
        @JsonLongString
        Long databaseId,
        @JsonLongString
        Long tableId,
        String nameLike
) {
}

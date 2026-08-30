package com.corwin.schemaforge.interfaces.web.req;

import com.corwin.framework.json.JsonLongString;
import com.corwin.framework.web.request.PageRuleRequest;
import com.corwin.framework.web.request.SortRuleRequest;

/**
 * @author Corwin 2026/2/24
 */
public record SchemaDdlPageReq(
    PageRuleRequest page,
    SortRuleRequest sort,
    @JsonLongString Long managedDatabaseId,
    String nameLike) {}

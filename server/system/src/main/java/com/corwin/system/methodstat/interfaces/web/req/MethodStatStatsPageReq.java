package com.corwin.system.methodstat.interfaces.web.req;

import com.corwin.framework.web.request.PageRuleRequest;
import com.corwin.framework.web.request.SortRuleRequest;

/**
 * Request DTO for paginated method statistics query with optional method name filtering and sorting.
 * @author Corwin 2026/3/25
 */
public record MethodStatStatsPageReq(
        PageRuleRequest page,
        SortRuleRequest sort,
        String methodName,
        String matchMode
) {
}

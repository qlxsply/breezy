package com.corwin.system.methodstat.interfaces.web.req;

import com.corwin.framework.web.request.PageRuleRequest;
import com.corwin.framework.web.request.SortRuleRequest;

/**
 * @author Corwin 2026/3/25
 */
public record MethodStatStatsPageReq(
        PageRuleRequest page,
        SortRuleRequest sort,
        String methodName,
        String matchMode
) {
}

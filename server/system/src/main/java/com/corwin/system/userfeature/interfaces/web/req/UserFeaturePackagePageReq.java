package com.corwin.system.userfeature.interfaces.web.req;

import com.corwin.framework.web.request.PageRuleRequest;
import com.corwin.framework.web.request.SortRuleRequest;

/**
 * @author Corwin 2026/6/14
 */
public record UserFeaturePackagePageReq(
        String keyword,
        Boolean enabled,
        PageRuleRequest page,
        SortRuleRequest sort
) {
}

package com.corwin.system.normalfeature.interfaces.web.req;

import com.corwin.framework.web.request.PageRuleRequest;
import com.corwin.framework.web.request.SortRuleRequest;

/**
 * @author Corwin 2026/5/21
 */
public record NormalFeatureGroupPageReq(
        PageRuleRequest page,
        SortRuleRequest sort,
        String keyword,
        Boolean enabled
) {
}

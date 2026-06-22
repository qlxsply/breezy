package com.corwin.system.dict.interfaces.web.req;

import com.corwin.framework.web.request.PageRuleRequest;
import com.corwin.framework.web.request.SortRuleRequest;

/**
 * @author Corwin 2026/3/16
 */
public record DictTypePageReq(
        PageRuleRequest page,
        SortRuleRequest sort,
        String code,
        String name
) {
}

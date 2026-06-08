package com.corwin.clinic.interfaces.web.req;

import com.corwin.framework.web.request.PageRuleRequest;
import com.corwin.framework.web.request.SortRuleRequest;

/**
 * @author Corwin 2026/2/8
 */
public record SupplierPageReq(
        PageRuleRequest page,
        SortRuleRequest sort,
        String nameLike,
        Boolean enabled
) {
}

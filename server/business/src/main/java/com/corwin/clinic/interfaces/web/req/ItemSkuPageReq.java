package com.corwin.clinic.interfaces.web.req;

import com.corwin.clinic.domain.model.ItemCategory;
import com.corwin.framework.web.request.PageRuleRequest;
import com.corwin.framework.web.request.SortRuleRequest;

/**
 * @author Corwin 2026/2/8
 */
public record ItemSkuPageReq(
        PageRuleRequest page,
        SortRuleRequest sort,
        ItemCategory category,
        String nameLike,
        Boolean enabled
) {
}

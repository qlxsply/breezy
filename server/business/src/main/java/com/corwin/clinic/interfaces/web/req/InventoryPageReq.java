package com.corwin.clinic.interfaces.web.req;

import com.corwin.framework.web.request.PageRuleRequest;
import com.corwin.framework.web.request.SortRuleRequest;

/**
 * @author Corwin 2026/2/9
 */
public record InventoryPageReq(
        PageRuleRequest page,
        SortRuleRequest sort,
        Long skuId
) {
}

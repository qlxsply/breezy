package com.corwin.clinic.interfaces.web.req;

import com.corwin.framework.web.request.PageRuleRequest;
import com.corwin.framework.web.request.SortRuleRequest;

import java.time.LocalDate;

/**
 * @author Corwin 2026/2/8
 */
public record SalePageReq(
        PageRuleRequest page,
        SortRuleRequest sort,
        String customerNameLike,
        LocalDate startAt,
        LocalDate endAt
) {
}

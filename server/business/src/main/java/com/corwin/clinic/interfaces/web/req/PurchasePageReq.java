package com.corwin.clinic.interfaces.web.req;

import com.corwin.framework.web.request.PageRuleRequest;
import com.corwin.framework.web.request.SortRuleRequest;

import java.time.LocalDate;

/**
 * @author Corwin 2026/2/8
 */
public record PurchasePageReq(
        PageRuleRequest page,
        SortRuleRequest sort,
        Long supplierId,
        LocalDate startAt,
        LocalDate endAt
) {
}

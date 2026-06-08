package com.corwin.clinic.interfaces.web.req;

import com.corwin.clinic.domain.model.LedgerBizType;
import com.corwin.framework.web.request.PageRuleRequest;
import com.corwin.framework.web.request.SortRuleRequest;

import java.time.LocalDate;

/**
 * @author Corwin 2026/2/8
 */
public record LedgerPageReq(
        PageRuleRequest page,
        SortRuleRequest sort,
        Long skuId,
        LedgerBizType bizType,
        Long supplierId,
        LocalDate startAt,
        LocalDate endAt
) {
}

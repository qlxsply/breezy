package com.corwin.system.auth.interfaces.web.req;

import com.corwin.framework.web.request.PageRuleRequest;
import com.corwin.framework.web.request.SortRuleRequest;

import java.time.Instant;

/**
 * Request DTO for paginated login log query.
 *
 * @author Corwin 2026/1/23
 */
public record LoginLogPageReq(
        PageRuleRequest page,
        SortRuleRequest sort,
        String userAccount,
        Instant startAt,
        Instant endAt
) {
}

package com.corwin.system.webuser.interfaces.web.req;

import com.corwin.framework.web.request.PageRuleRequest;
import com.corwin.framework.web.request.SortRuleRequest;

/**
 * Page request DTO for external user listing with keyword and status filter.
 *
 * @author Corwin 2026/5/11
 */
public record WebUserPageReq(
        PageRuleRequest page,
        SortRuleRequest sort,
        String keyword,
        String status
) {
}

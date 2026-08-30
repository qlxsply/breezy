package com.corwin.system.role.interfaces.web.req;

import com.corwin.framework.web.request.PageRuleRequest;
import com.corwin.framework.web.request.SortRuleRequest;

/**
 * Request DTO for paginated role listing with optional keyword and enabled filter.
 *
 * @author Corwin 2026/7/7
 */
public record RolePageReq(
    PageRuleRequest page, SortRuleRequest sort, String keyword, Boolean enabled) {}

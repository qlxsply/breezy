package com.corwin.system.userfeature.interfaces.web.req;

import com.corwin.framework.web.request.PageRuleRequest;
import com.corwin.framework.web.request.SortRuleRequest;

/**
 * Request DTO for paginated user application package queries.
 *
 * @param keyword optional keyword filter
 * @param enabled optional enabled status filter
 * @param page the page request parameters
 * @param sort the sort rule
 * @author Corwin 2026/6/14
 */
public record UserFeaturePackagePageReq(
    String keyword, Boolean enabled, PageRuleRequest page, SortRuleRequest sort) {}

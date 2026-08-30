package com.corwin.system.user.interfaces.web.req;

import com.corwin.framework.web.request.PageRuleRequest;

/**
 * Request object for paginating an admin user's login activity history.
 *
 * @author Corwin 2026/7/29
 */
public record AdminProfileLoginActivityPageReq(PageRuleRequest page) {}

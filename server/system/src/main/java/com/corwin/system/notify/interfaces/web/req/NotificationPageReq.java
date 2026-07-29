package com.corwin.system.notify.interfaces.web.req;

import com.corwin.framework.web.request.PageRuleRequest;

/**
 * @author Corwin 2026/7/29
 */
public record NotificationPageReq(
        PageRuleRequest page,
        String status
) {
}

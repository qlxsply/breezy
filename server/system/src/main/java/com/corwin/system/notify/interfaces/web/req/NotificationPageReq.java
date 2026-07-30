package com.corwin.system.notify.interfaces.web.req;

import com.corwin.framework.web.request.PageRuleRequest;

/**
 * Request DTO for paginated notification queries.
 *
 * @param page   the page specification
 * @param status the filter status ("all" or "unread")
 * @author Corwin 2026/7/29
 */
public record NotificationPageReq(
        PageRuleRequest page,
        String status
) {
}

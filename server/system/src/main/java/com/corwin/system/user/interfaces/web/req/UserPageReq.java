package com.corwin.system.user.interfaces.web.req;

import com.corwin.framework.web.request.PageRuleRequest;
import com.corwin.framework.web.request.SortRuleRequest;
import com.corwin.system.user.domain.model.UserStatus;

/**
 * @author Corwin 2026/1/28
 */
public record UserPageReq(
        PageRuleRequest page,
        SortRuleRequest sort,
        String usernameLike,
        UserStatus status
) {
}

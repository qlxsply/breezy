package com.corwin.system.resource.interfaces.web.req;

import com.corwin.framework.web.request.PageRuleRequest;

/**
 * @author Corwin 2026/7/29
 */
public record ApiPageReq(
        PageRuleRequest page,
        String module,
        String pathPattern,
        String handlerClass,
        String handlerMethod,
        String permissionDeclared,
        String accessType,
        String userType,
        String auditDeclared,
        String status
) {
}

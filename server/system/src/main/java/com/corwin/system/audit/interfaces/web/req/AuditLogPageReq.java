package com.corwin.system.audit.interfaces.web.req;

import com.corwin.framework.web.request.PageRuleRequest;
import com.corwin.framework.web.request.SortRuleRequest;
import com.corwin.system.audit.domain.model.AuditLevel;

import java.time.Instant;

/**
 * @author Corwin 2026/4/19
 */
public record AuditLogPageReq(
        PageRuleRequest page,
        SortRuleRequest sort,
        String traceId,
        Long operatorUserId,
        String operatorUsername,
        String applicationCode,
        String requestUri,
        String auditResource,
        String auditAction,
        AuditLevel auditLevel,
        Boolean success,
        Instant startAt,
        Instant endAt
) {
}

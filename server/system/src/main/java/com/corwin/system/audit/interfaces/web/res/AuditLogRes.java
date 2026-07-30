package com.corwin.system.audit.interfaces.web.res;

import com.corwin.framework.json.JsonLongString;
import com.corwin.system.audit.domain.model.AuditLevel;
import com.corwin.system.resource.domain.model.ApiMethod;
import com.corwin.system.resource.domain.model.ApiProtocol;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for audit log entries.
 * Contains all fields of an audit log suitable for API serialization,
 * with {@link JsonLongString} annotations for safe long-number transport.
 *
 * @author Corwin 2026/4/19
 */
public record AuditLogRes(
        @JsonLongString
        Long id,
        String traceId,
        String requestId,
        @JsonLongString
        Long operatorUserId,
        String operatorUsername,
        String operatorUserType,
        String applicationCode,
        ApiProtocol protocol,
        ApiMethod httpMethod,
        String pathPattern,
        String requestUri,
        List<String> permissionCodes,
        String auditResource,
        String auditAction,
        String auditDescription,
        AuditLevel auditLevel,
        String requestIp,
        String userAgent,
        String requestParamSummary,
        String requestBodySummary,
        String responseSummary,
        boolean success,
        String errorCode,
        String errorMessage,
        Instant startedAt,
        Instant endedAt,
        Long durationMs,
        Instant createdAt
) {
}

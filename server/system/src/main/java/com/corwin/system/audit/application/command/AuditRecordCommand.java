package com.corwin.system.audit.application.command;

import com.corwin.system.audit.domain.model.AuditLevel;
import com.corwin.system.resource.domain.model.ApiMethod;
import com.corwin.system.resource.domain.model.ApiProtocol;
import java.time.Instant;

/**
 * Command for recording an audit log entry. Carries all data needed to persist a single audit trail
 * record, including operator identity, request metadata, and execution outcome.
 *
 * @author Corwin 2026/4/19
 */
public record AuditRecordCommand(
    String traceId,
    String requestId,
    Long operatorUserId,
    String operatorUsername,
    String operatorUserType,
    String applicationCode,
    ApiProtocol protocol,
    ApiMethod httpMethod,
    String pathPattern,
    String requestUri,
    String permissionCodes,
    String auditResource,
    String auditAction,
    String auditDescription,
    AuditLevel auditLevel,
    String requestIp,
    String userAgent,
    String requestParamSummary,
    String requestBodySummary,
    String responseSummary,
    Boolean success,
    String errorCode,
    String errorMessage,
    Instant startedAt,
    Instant endedAt,
    Long durationMs,
    Instant createdAt) {}

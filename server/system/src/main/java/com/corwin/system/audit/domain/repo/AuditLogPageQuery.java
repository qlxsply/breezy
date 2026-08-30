package com.corwin.system.audit.domain.repo;

import com.corwin.system.audit.domain.model.AuditLevel;
import java.time.Instant;

/**
 * Page query parameters for filtering audit log entries. Supports filtering by trace ID, operator,
 * application, resource, action, level, success status, and a time range.
 *
 * @author Corwin 2026/4/19
 */
public record AuditLogPageQuery(
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
    Instant endAt) {}

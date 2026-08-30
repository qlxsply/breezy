package com.corwin.system.audit.infrastructure.aop;

import com.corwin.system.audit.domain.model.AuditLevel;

/**
 * Resolved audit operation metadata. Holds the resource, action, description, severity level,
 * recording flags, and application code derived from {@link Audit} and {@code ApiMeta} annotations.
 *
 * @author Corwin 2026/4/19
 */
public record AuditOperation(
    String resource,
    String action,
    String description,
    AuditLevel level,
    boolean recordRequest,
    boolean recordResponse,
    String applicationCode) {}

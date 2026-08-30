package com.corwin.system.diagnostic.interfaces.web.req;

import com.corwin.system.diagnostic.domain.model.DiagnosticEventType;

/**
 * Request DTO for querying diagnostic events with optional type filtering.
 *
 * @author Corwin 2026/7/29
 */
public record DiagnosticEventListReq(Integer limit, DiagnosticEventType type) {}

package com.corwin.system.diagnostic.interfaces.web.req;

import com.corwin.system.diagnostic.domain.model.DiagnosticEventType;

/**
 * @author Corwin 2026/7/29
 */
public record DiagnosticEventListReq(
        Integer limit,
        DiagnosticEventType type
) {
}

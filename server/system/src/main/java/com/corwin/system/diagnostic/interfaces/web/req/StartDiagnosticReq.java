package com.corwin.system.diagnostic.interfaces.web.req;

import com.corwin.system.diagnostic.domain.model.DiagnosticItem;

import java.util.Set;

/**
 * @author Corwin 2026/4/16
 */
public record StartDiagnosticReq(
        Long intervalMs,
        Integer historyCapacity,
        Integer eventCapacity,
        Set<DiagnosticItem> items,
        Boolean deepMode,
        Long slowRequestThresholdMs,
        Long slowSqlThresholdMs,
        Long ttlSeconds
) {
}

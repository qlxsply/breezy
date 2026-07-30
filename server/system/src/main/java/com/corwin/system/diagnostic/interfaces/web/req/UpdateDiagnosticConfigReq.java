package com.corwin.system.diagnostic.interfaces.web.req;

import com.corwin.system.diagnostic.domain.model.DiagnosticItem;

import java.util.Set;

/**
 * Request DTO for updating the configuration of a running diagnostic session.
 *
 * @author Corwin 2026/4/16
 */
public record UpdateDiagnosticConfigReq(
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

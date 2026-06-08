package com.corwin.system.diagnostic.application.command;

import com.corwin.system.diagnostic.domain.model.DiagnosticItem;

import java.util.Set;

/**
 * @author Corwin 2026/4/16
 */
public record StartDiagnosticCommand(
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

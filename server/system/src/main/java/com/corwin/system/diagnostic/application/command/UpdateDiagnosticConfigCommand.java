package com.corwin.system.diagnostic.application.command;

import com.corwin.system.diagnostic.domain.model.DiagnosticItem;
import java.util.Set;

/**
 * Command object for updating the diagnostic session configuration.
 *
 * @author Corwin 2026/4/16
 */
public record UpdateDiagnosticConfigCommand(
    Long intervalMs,
    Integer historyCapacity,
    Integer eventCapacity,
    Set<DiagnosticItem> items,
    Boolean deepMode,
    Long slowRequestThresholdMs,
    Long slowSqlThresholdMs,
    Long ttlSeconds) {}

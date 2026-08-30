package com.corwin.system.diagnostic.application.view;

import com.corwin.system.diagnostic.domain.model.DiagnosticConfig;
import com.corwin.system.diagnostic.domain.model.DiagnosticStatus;
import java.time.Instant;

/**
 * View object representing the current diagnostic session state exposed to clients.
 *
 * @author Corwin 2026/4/16
 */
public record DiagnosticSessionView(
    DiagnosticStatus status,
    DiagnosticConfig config,
    Instant startedAt,
    Instant expireAt,
    long remainingTtlSeconds) {}

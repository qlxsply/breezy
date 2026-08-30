package com.corwin.system.diagnostic.domain.model;

import java.time.Instant;

/**
 * A point-in-time snapshot of all collected diagnostic data across enabled items.
 *
 * @author Corwin 2026/4/16
 */
public record DiagnosticSnapshot(
    Instant capturedAt,
    JvmSnapshot jvm,
    OsSnapshot os,
    ThreadSnapshot thread,
    HttpSnapshot http,
    DbPoolSnapshot dbPool,
    SqlSnapshot sql,
    JfrSnapshot jfr) {}

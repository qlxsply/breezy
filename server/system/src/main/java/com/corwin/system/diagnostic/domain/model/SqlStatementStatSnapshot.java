package com.corwin.system.diagnostic.domain.model;

/**
 * Per-statement SQL execution statistics for a specific data source.
 *
 * @author Corwin 2026/4/16
 */
public record SqlStatementStatSnapshot(
    String dataSourceName,
    String sql,
    long totalExecutions,
    long totalDurationMs,
    long averageDurationMs,
    long slowExecutions,
    long errorExecutions,
    long maxDurationMs) {}

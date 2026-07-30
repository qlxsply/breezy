package com.corwin.system.diagnostic.domain.model;

import java.util.List;

/**
 * Snapshot of SQL execution metrics including total executions, average duration, and slow/error counts.
 *
 * @author Corwin 2026/4/16
 */
public record SqlSnapshot(
        long totalExecutions,
        double averageDurationMs,
        long slowSqlCount,
        long errorCount,
        List<SqlStatementStatSnapshot> topStatements
) {

    /**
     * Compact constructor that normalises a null top-statements list to an immutable empty list.
     */
    public SqlSnapshot {
        topStatements = topStatements == null ? List.of() : List.copyOf(topStatements);
    }
}

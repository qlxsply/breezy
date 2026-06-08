package com.corwin.system.diagnostic.domain.model;

import java.util.List;

/**
 * @author Corwin 2026/4/16
 */
public record SqlSnapshot(
        long totalExecutions,
        double averageDurationMs,
        long slowSqlCount,
        long errorCount,
        List<SqlStatementStatSnapshot> topStatements
) {

    public SqlSnapshot {
        topStatements = topStatements == null ? List.of() : List.copyOf(topStatements);
    }
}

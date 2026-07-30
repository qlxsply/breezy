package com.corwin.system.diagnostic.domain.model;

/**
 * Per-pool database connection pool snapshot for a single registered DataSource.
 *
 * @author Corwin 2026/4/16
 */
public record DbPoolEntrySnapshot(
        String beanName,
        String poolName,
        int activeConnections,
        int idleConnections,
        int totalConnections,
        int waitingThreads
) {
}

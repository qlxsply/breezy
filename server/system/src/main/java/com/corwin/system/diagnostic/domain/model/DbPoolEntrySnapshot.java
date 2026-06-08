package com.corwin.system.diagnostic.domain.model;

/**
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

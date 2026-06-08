package com.corwin.system.diagnostic.domain.model;

import java.util.List;

/**
 * @author Corwin 2026/4/16
 */
public record DbPoolSnapshot(
        int poolCount,
        int activeConnections,
        int idleConnections,
        int totalConnections,
        int waitingThreads,
        List<DbPoolEntrySnapshot> pools
) {

    public DbPoolSnapshot {
        pools = pools == null ? List.of() : List.copyOf(pools);
    }
}

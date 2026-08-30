package com.corwin.system.diagnostic.domain.model;

import java.util.List;

/**
 * Snapshot of database connection pool state aggregated across all registered pools.
 *
 * @author Corwin 2026/4/16
 */
public record DbPoolSnapshot(
    int poolCount,
    int activeConnections,
    int idleConnections,
    int totalConnections,
    int waitingThreads,
    List<DbPoolEntrySnapshot> pools) {

  /** Compact constructor that normalises a null pools list to an immutable empty list. */
  public DbPoolSnapshot {
    pools = pools == null ? List.of() : List.copyOf(pools);
  }
}

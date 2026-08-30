package com.corwin.system.diagnostic.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * Snapshot of JVM runtime state including memory, class loading, CPU, and garbage collection
 * metrics.
 *
 * @author Corwin 2026/4/16
 */
public record JvmSnapshot(
    Instant startedAt,
    long uptimeMs,
    String javaVersion,
    List<String> inputArguments,
    long heapUsedBytes,
    long heapCommittedBytes,
    long heapMaxBytes,
    long nonHeapUsedBytes,
    long nonHeapCommittedBytes,
    long nonHeapMaxBytes,
    List<MemoryPoolSnapshot> memoryPools,
    long loadedClassCount,
    long totalLoadedClassCount,
    long unloadedClassCount,
    double processCpuLoad,
    long processCpuTimeMs,
    long gcCollectionCount,
    long gcCollectionTimeMs) {

  /** Compact constructor that normalises null collections to immutable empty lists. */
  public JvmSnapshot {
    inputArguments = inputArguments == null ? List.of() : List.copyOf(inputArguments);
    memoryPools = memoryPools == null ? List.of() : List.copyOf(memoryPools);
  }
}

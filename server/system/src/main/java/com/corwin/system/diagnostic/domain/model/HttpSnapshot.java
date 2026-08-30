package com.corwin.system.diagnostic.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Snapshot of HTTP request metrics including in-flight count, totals, status distribution, and
 * latency percentiles.
 *
 * @author Corwin 2026/4/16
 */
public record HttpSnapshot(
    int inFlightRequests,
    long totalRequests,
    long averageDurationMs,
    long slowRequestCount,
    long errorRequestCount,
    Map<Integer, Long> statusCounts,
    long p95DurationMs,
    long p99DurationMs,
    List<HttpUriStatSnapshot> topUris) {

  /** Compact constructor that normalises null collections to immutable empty ones. */
  public HttpSnapshot {
    statusCounts = statusCounts == null ? Map.of() : Map.copyOf(statusCounts);
    topUris = topUris == null ? List.of() : List.copyOf(topUris);
  }
}

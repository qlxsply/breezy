package com.corwin.system.methodstat.domain.model;

/**
 * Domain record holding duration distribution metrics (min, max, avg, and percentiles) computed
 * from a sliding sample of method execution times.
 *
 * @author Corwin 2026/3/25
 */
public record MethodStatDurationMetrics(
    int sampleSize, long min, long max, double avg, long p50, long p90, long p95, long p99) {

  /**
   * Return an empty metrics instance with all values set to zero.
   *
   * @return empty duration metrics
   */
  public static MethodStatDurationMetrics empty() {
    return new MethodStatDurationMetrics(0, 0L, 0L, 0D, 0L, 0L, 0L, 0L);
  }
}

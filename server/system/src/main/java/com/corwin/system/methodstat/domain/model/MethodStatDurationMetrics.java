package com.corwin.system.methodstat.domain.model;

/**
 * @author Corwin 2026/3/25
 */
public record MethodStatDurationMetrics(
        int sampleSize,
        long min,
        long max,
        double avg,
        long p50,
        long p90,
        long p95,
        long p99
) {

    public static MethodStatDurationMetrics empty() {
        return new MethodStatDurationMetrics(0, 0L, 0L, 0D, 0L, 0L, 0L, 0L);
    }
}

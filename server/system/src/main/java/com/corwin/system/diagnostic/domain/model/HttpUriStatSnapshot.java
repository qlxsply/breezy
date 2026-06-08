package com.corwin.system.diagnostic.domain.model;

/**
 * @author Corwin 2026/4/16
 */
public record HttpUriStatSnapshot(
        String method,
        String uri,
        long totalRequests,
        long totalDurationMs,
        long averageDurationMs,
        long slowRequestCount,
        long errorRequestCount
) {
}

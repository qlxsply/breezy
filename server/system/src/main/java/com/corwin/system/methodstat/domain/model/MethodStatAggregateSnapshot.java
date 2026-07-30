package com.corwin.system.methodstat.domain.model;

import java.util.Objects;

/**
 * Immutable snapshot of aggregate method statistics at a point in time, including total and
 * recent call counts across multiple time windows and duration metrics.
 * @author Corwin 2026/3/25
 */
public record MethodStatAggregateSnapshot(
        MethodStatKey key,
        long totalCalls,
        long totalSuccess,
        long totalFailure,
        long recent1MinuteCalls,
        long recent1HourCalls,
        long recent1DayCalls,
        long recent1MinuteSuccess,
        long recent1MinuteFailure,
        long recent1HourSuccess,
        long recent1HourFailure,
        long recent1DaySuccess,
        long recent1DayFailure,
        MethodStatDurationMetrics durationMetrics
) {

    public MethodStatAggregateSnapshot {
        Objects.requireNonNull(key, "key required");
        Objects.requireNonNull(durationMetrics, "durationMetrics required");
    }
}

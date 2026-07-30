package com.corwin.system.diagnostic.domain.model;

/**
 * Snapshot of JFR (Java Flight Recorder) event counters since the last reset.
 *
 * @author Corwin 2026/4/16
 */
public record JfrSnapshot(
        boolean running,
        long gcEventCount,
        long gcPauseTimeMs,
        long exceptionEventCount,
        long threadParkEventCount,
        long monitorBlockedEventCount
) {
}

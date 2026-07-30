package com.corwin.system.diagnostic.domain.model;

import java.util.List;

/**
 * Snapshot of JVM thread state including counts by state and deadlocked thread IDs.
 *
 * @author Corwin 2026/4/16
 */
public record ThreadSnapshot(
        int threadCount,
        int daemonThreadCount,
        int peakThreadCount,
        long totalStartedThreadCount,
        int runnableCount,
        int blockedCount,
        int waitingCount,
        int timedWaitingCount,
        List<Long> deadlockedThreadIds
) {

    /**
     * Compact constructor that normalises a null deadlocked thread ID list to an immutable empty list.
     */
    public ThreadSnapshot {
        deadlockedThreadIds = deadlockedThreadIds == null ? List.of() : List.copyOf(deadlockedThreadIds);
    }
}

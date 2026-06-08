package com.corwin.system.diagnostic.domain.model;

import java.util.List;

/**
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

    public ThreadSnapshot {
        deadlockedThreadIds = deadlockedThreadIds == null ? List.of() : List.copyOf(deadlockedThreadIds);
    }
}

package com.corwin.system.diagnostic.infrastructure.runtime;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

/**
 * @author Corwin 2026/4/16
 */
@Component
@RequiredArgsConstructor
public class DiagnosticScheduler {

    private final ThreadPoolTaskScheduler taskScheduler;

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long intervalMs) {
        return taskScheduler.scheduleAtFixedRate(task, Duration.ofMillis(intervalMs));
    }

    public void cancel(ScheduledFuture<?> future) {
        if (future != null) {
            future.cancel(false);
        }
    }
}

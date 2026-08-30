package com.corwin.system.diagnostic.infrastructure.runtime;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

/**
 * Wrapper around Spring's ThreadPoolTaskScheduler for scheduling diagnostic snapshot tasks.
 *
 * @author Corwin 2026/4/16
 */
@Component
@RequiredArgsConstructor
public class DiagnosticScheduler {

  private final ThreadPoolTaskScheduler taskScheduler;

  /**
   * Schedules the given task to run at a fixed rate with the specified interval.
   *
   * @param task the task to execute
   * @param intervalMs the interval in milliseconds
   * @return a ScheduledFuture for the scheduled task
   */
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long intervalMs) {
    return taskScheduler.scheduleAtFixedRate(task, Duration.ofMillis(intervalMs));
  }

  /**
   * Cancels a previously scheduled future task.
   *
   * @param future the future to cancel, may be null
   */
  public void cancel(ScheduledFuture<?> future) {
    if (future != null) {
      future.cancel(false);
    }
  }
}

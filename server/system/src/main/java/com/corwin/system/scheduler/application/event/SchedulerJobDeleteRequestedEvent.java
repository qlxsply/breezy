package com.corwin.system.scheduler.application.event;

/**
 * Event fired when a job deletion is requested.
 *
 * @author Corwin 2026/4/15
 */
public record SchedulerJobDeleteRequestedEvent(
        String jobId
) {
}

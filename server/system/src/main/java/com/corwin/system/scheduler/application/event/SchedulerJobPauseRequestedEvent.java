package com.corwin.system.scheduler.application.event;

/**
 * Event fired when a job pause is requested.
 *
 * @author Corwin 2026/4/15
 */
public record SchedulerJobPauseRequestedEvent(
        String jobId
) {
}

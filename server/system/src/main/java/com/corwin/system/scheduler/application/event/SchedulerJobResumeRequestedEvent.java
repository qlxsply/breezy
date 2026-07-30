package com.corwin.system.scheduler.application.event;

/**
 * Event fired when a job resume is requested.
 *
 * @author Corwin 2026/4/15
 */
public record SchedulerJobResumeRequestedEvent(
        String jobId
) {
}

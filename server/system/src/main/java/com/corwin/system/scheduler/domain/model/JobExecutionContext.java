package com.corwin.system.scheduler.domain.model;

import java.time.Instant;

/**
 * Record providing execution context information to a job handler.
 *
 * @author Corwin 2026/4/15
 */
public record JobExecutionContext<P extends JobPayload>(
    String jobId,
    String executionId,
    Instant scheduledTime,
    Instant startTime,
    SchedulerTriggerType triggerType,
    P payload) {}

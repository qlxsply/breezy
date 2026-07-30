package com.corwin.system.scheduler.application.view;

import com.corwin.system.scheduler.domain.model.SchedulerExecutionResult;
import com.corwin.system.scheduler.domain.model.SchedulerTriggerType;

import java.time.Instant;

/**
 * View of a single job execution record.
 *
 * @author Corwin 2026/4/15
 */
public record SchedulerJobExecutionView(
        String executionId,
        String jobId,
        Instant scheduledTime,
        Instant startTime,
        Instant endTime,
        SchedulerExecutionResult result,
        Long durationMs,
        SchedulerTriggerType triggerType,
        String resultCode,
        String resultMessage,
        String errorMessage
) {
}

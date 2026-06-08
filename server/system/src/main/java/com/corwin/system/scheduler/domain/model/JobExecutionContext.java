package com.corwin.system.scheduler.domain.model;

import java.time.Instant;

/**
 * 任务执行上下文。
 *
 * @author Corwin 2026/4/15
 */
public record JobExecutionContext<P extends JobPayload>(
        String jobId,
        String executionId,
        Instant scheduledTime,
        Instant startTime,
        SchedulerTriggerType triggerType,
        P payload
) {
}

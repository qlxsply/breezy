package com.corwin.system.scheduler.application.view;

import com.corwin.system.scheduler.domain.model.SchedulerJobSource;
import com.corwin.system.scheduler.domain.model.SchedulerJobStatus;
import com.corwin.system.scheduler.domain.model.SchedulerJobType;

import java.time.Instant;

/**
 * @author Corwin 2026/4/15
 */
public record SchedulerJobDetailView(
        String jobId,
        String namespace,
        String name,
        SchedulerJobSource source,
        SchedulerJobType jobType,
        String handlerKey,
        String scheduleRuleType,
        String payloadType,
        boolean enabled,
        boolean deleted,
        boolean allowConcurrent,
        Integer versionNo,
        String remark,
        SchedulerJobStatus status,
        Instant nextFireTime,
        Instant lastFireTime,
        Instant lastSuccessTime,
        Instant lastFailureTime,
        Long consecutiveFailures,
        String lastErrorMessage,
        Long lastDurationMs,
        String currentExecutionId,
        Instant createdAt,
        Instant updatedAt
) {
}

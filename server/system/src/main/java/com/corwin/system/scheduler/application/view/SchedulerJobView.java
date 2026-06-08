package com.corwin.system.scheduler.application.view;

import com.corwin.system.scheduler.domain.model.SchedulerJobStatus;
import com.corwin.system.scheduler.domain.model.SchedulerJobType;

import java.time.Instant;

/**
 * @author Corwin 2026/4/15
 */
public record SchedulerJobView(
        String jobId,
        String namespace,
        String name,
        SchedulerJobType jobType,
        String handlerKey,
        String scheduleRuleType,
        boolean enabled,
        boolean allowConcurrent,
        String remark,
        SchedulerJobStatus status,
        Instant nextFireTime,
        Instant lastFireTime,
        Instant lastSuccessTime,
        Instant lastFailureTime,
        String lastErrorMessage,
        Instant updatedAt
) {
}

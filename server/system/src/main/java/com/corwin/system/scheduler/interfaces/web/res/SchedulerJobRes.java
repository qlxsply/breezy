package com.corwin.system.scheduler.interfaces.web.res;

import com.corwin.system.scheduler.domain.model.SchedulerJobStatus;
import com.corwin.system.scheduler.domain.model.SchedulerJobType;
import java.time.Instant;

/**
 * Response DTO for a scheduled job list item.
 *
 * @author Corwin 2026/4/15
 */
public record SchedulerJobRes(
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
    Instant updatedAt) {}

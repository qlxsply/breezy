package com.corwin.system.task.interfaces.web.res;

import com.corwin.system.task.domain.model.TaskStatus;

import java.time.Instant;

/**
 * 任务管理响应 DTO
 *
 * @author Corwin 2026/3/30
 */
public record TaskRes(
        String code,
        String name,
        String description,
        String beanName,
        String methodName,
        boolean removed,
        String cronExpr,
        TaskStatus status,
        Instant lastRunAt,
        String lastRunStatus,
        String lastErrorMessage,
        Instant updatedAt
) {
}

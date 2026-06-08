package com.corwin.system.task.application.view;

import com.corwin.system.task.domain.model.TaskStatus;

import java.time.Instant;

/**
 * @author Corwin 2026/3/30
 */
public record TaskView(
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

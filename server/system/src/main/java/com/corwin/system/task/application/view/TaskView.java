package com.corwin.system.task.application.view;

import com.corwin.system.task.domain.model.TaskStatus;

import java.time.Instant;

/**
 * View object representing a task definition combined with its runtime configuration.
 *
 * @param code             the unique task code
 * @param name             the task name
 * @param description      the task description
 * @param beanName         the Spring bean name that contains the task method
 * @param methodName       the method name annotated as a task
 * @param removed          whether the task definition has been removed
 * @param cronExpr         the cron expression for scheduling
 * @param status           the current task status
 * @param lastRunAt        the timestamp of the last execution
 * @param lastRunStatus    the status of the last execution
 * @param lastErrorMessage the error message from the last failed execution
 * @param updatedAt        the timestamp of the last update to the definition
 *
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

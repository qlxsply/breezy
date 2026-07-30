package com.corwin.system.task.interfaces.web.req;

/**
 * Request DTO for updating a task's cron configuration.
 *
 * @param cronExpr the new cron expression for scheduling
 *
 * @author Corwin 2026/3/30
 */
public record UpdateTaskConfigReq(String cronExpr) {
}

package com.corwin.system.scheduler.domain.model;

/**
 * 任务运行状态。
 *
 * @author Corwin 2026/4/15
 */
public enum SchedulerJobStatus {
    SCHEDULED,
    RUNNING,
    PAUSED,
    CANCELLED,
    INVALID,
    ERROR
}

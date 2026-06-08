package com.corwin.system.scheduler.application.event;

/**
 * 动态任务恢复请求事件。
 *
 * @author Corwin 2026/4/15
 */
public record SchedulerJobResumeRequestedEvent(
        String jobId
) {
}

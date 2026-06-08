package com.corwin.system.scheduler.application.event;

/**
 * 动态任务删除请求事件。
 *
 * @author Corwin 2026/4/15
 */
public record SchedulerJobDeleteRequestedEvent(
        String jobId
) {
}

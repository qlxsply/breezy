package com.corwin.system.scheduler.application.event;

/**
 * 动态任务暂停请求事件。
 *
 * @author Corwin 2026/4/15
 */
public record SchedulerJobPauseRequestedEvent(
        String jobId
) {
}

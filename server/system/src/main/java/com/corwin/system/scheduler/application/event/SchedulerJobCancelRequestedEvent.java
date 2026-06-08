package com.corwin.system.scheduler.application.event;

/**
 * 动态任务取消请求事件。
 *
 * @author Corwin 2026/4/15
 */
public record SchedulerJobCancelRequestedEvent(
        String jobId
) {
}

package com.corwin.system.scheduler.domain.model;

/**
 * 动态任务处理器。
 *
 * @author Corwin 2026/4/15
 */
public interface SchedulerJobHandler<P extends JobPayload> {

    HandlerKey key();

    Class<P> payloadType();

    JobResult execute(JobExecutionContext<P> context) throws Exception;
}

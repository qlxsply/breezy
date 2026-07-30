package com.corwin.system.scheduler.domain.model;

/**
 * Interface for executing a scheduled job with a typed payload.
 *
 * @author Corwin 2026/4/15
 */
public interface SchedulerJobHandler<P extends JobPayload> {

    /**
     * Returns the unique handler key identifying this handler.
     *
     * @return the handler key
     */
    HandlerKey key();

    /**
     * Returns the expected payload type class.
     *
     * @return the payload type
     */
    Class<P> payloadType();

    /**
     * Executes the job with the given context.
     *
     * @param context the job execution context
     * @return the job result
     * @throws Exception if execution fails
     */
    JobResult execute(JobExecutionContext<P> context) throws Exception;
}

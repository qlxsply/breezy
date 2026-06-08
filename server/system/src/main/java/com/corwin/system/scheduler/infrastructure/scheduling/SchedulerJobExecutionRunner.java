package com.corwin.system.scheduler.infrastructure.scheduling;

import com.corwin.framework.util.HighDate;
import com.corwin.system.scheduler.domain.model.JobExecutionContext;
import com.corwin.system.scheduler.domain.model.JobPayload;
import com.corwin.system.scheduler.domain.model.JobResult;
import com.corwin.system.scheduler.domain.model.ScheduleRule;
import com.corwin.system.scheduler.domain.model.SchedulerJobExecution;
import com.corwin.system.scheduler.domain.model.SchedulerJobHandler;
import com.corwin.system.scheduler.domain.model.SchedulerJobRuntime;
import com.corwin.system.scheduler.domain.model.SchedulerTriggerType;
import com.corwin.system.scheduler.domain.repo.SchedulerJobExecutionRepository;
import com.corwin.system.scheduler.domain.repo.SchedulerJobRuntimeRepository;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 单次任务执行器。
 *
 * @author Corwin 2026/4/15
 */
public class SchedulerJobExecutionRunner implements Runnable {

    private final String jobId;
    private final SchedulerJobHandler<JobPayload> handler;
    private final JobPayload payload;
    private final ScheduleRule scheduleRule;
    private final SchedulerTriggerType triggerType;
    private final SchedulerJobRuntimeRepository runtimeRepository;
    private final SchedulerJobExecutionRepository executionRepository;
    private final ReentrantLock executionLock;

    @SuppressWarnings("unchecked")
    public SchedulerJobExecutionRunner(String jobId, SchedulerJobHandler<?> handler, JobPayload payload,
            ScheduleRule scheduleRule, SchedulerTriggerType triggerType, SchedulerJobRuntimeRepository runtimeRepository,
            SchedulerJobExecutionRepository executionRepository, ReentrantLock executionLock) {
        this.jobId = jobId;
        this.handler = (SchedulerJobHandler<JobPayload>) handler;
        this.payload = payload;
        this.scheduleRule = scheduleRule;
        this.triggerType = triggerType;
        this.runtimeRepository = runtimeRepository;
        this.executionRepository = executionRepository;
        this.executionLock = executionLock;
    }

    @Override
    public void run() {
        Instant scheduledTime = HighDate.mockInstant();
        Instant startTime = HighDate.mockInstant();
        String executionId = UUID.randomUUID().toString().replace("-", "");

        boolean locked = executionLock == null || executionLock.tryLock();
        if (!locked) {
            SchedulerJobExecution skipped = SchedulerJobExecution.start(executionId, jobId, scheduledTime, startTime,
                    triggerType);
            skipped.finishSkipped(HighDate.mockInstant(), "Skipped because previous execution is still running");
            executionRepository.save(skipped);
            runtimeRepository.findById(jobId).ifPresent(runtime -> {
                runtime.markScheduled(ScheduleRuleTrigger.nextExecutionAfter(scheduleRule, scheduledTime, startTime));
                runtimeRepository.save(runtime);
            });
            return;
        }

        try {
            SchedulerJobExecution execution = SchedulerJobExecution.start(executionId, jobId, scheduledTime, startTime,
                    triggerType);
            executionRepository.save(execution);

            SchedulerJobRuntime runtime = runtimeRepository.findById(jobId)
                    .orElseGet(() -> SchedulerJobRuntime.create(jobId, null, null));
            runtime.markRunning(executionId, scheduledTime);
            runtimeRepository.save(runtime);

            JobResult result = handler.execute(new JobExecutionContext<>(jobId, executionId, scheduledTime, startTime,
                    triggerType, payload));
            Instant endTime = HighDate.mockInstant();
            long durationMs = HighDate.realTimestampMillis() - startTime.toEpochMilli();
            execution.finishSuccess(endTime, durationMs, result);
            executionRepository.save(execution);
            runtime.markSuccess(ScheduleRuleTrigger.nextExecutionAfter(scheduleRule, scheduledTime, endTime), endTime,
                    durationMs);
            runtimeRepository.save(runtime);
        } catch (Exception ex) {
            Instant endTime = HighDate.mockInstant();
            long durationMs = HighDate.realTimestampMillis() - startTime.toEpochMilli();
            SchedulerJobExecution execution = executionRepository.findById(executionId)
                    .orElseGet(() -> SchedulerJobExecution.start(executionId, jobId, scheduledTime, startTime,
                            triggerType));
            execution.finishFailure(endTime, durationMs, ex.getMessage(), stackTrace(ex));
            executionRepository.save(execution);

            SchedulerJobRuntime runtime = runtimeRepository.findById(jobId)
                    .orElseGet(() -> SchedulerJobRuntime.create(jobId, null, null));
            runtime.markFailure(ScheduleRuleTrigger.nextExecutionAfter(scheduleRule, scheduledTime, endTime), endTime,
                    durationMs, ex.getMessage());
            runtimeRepository.save(runtime);
        } finally {
            if (executionLock != null) {
                executionLock.unlock();
            }
        }
    }

    private String stackTrace(Exception ex) {
        StringWriter writer = new StringWriter();
        ex.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}

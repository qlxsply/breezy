package com.corwin.system.scheduler.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 任务执行历史。
 *
 * @author Corwin 2026/4/15
 */
@Entity
@Table(name = "sched_job_execution")
public class SchedulerJobExecution {

    @Id
    @Column(name = "execution_id", length = 64)
    private String executionId;

    @Column(name = "job_id", nullable = false, length = 128)
    private String jobId;

    @Column(name = "scheduled_time")
    private Instant scheduledTime;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_result", length = 32)
    private SchedulerExecutionResult result;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 32)
    private SchedulerTriggerType triggerType;

    @Column(name = "result_code", length = 64)
    private String resultCode;

    @Column(name = "result_message", length = 2000)
    private String resultMessage;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Lob
    @Column(name = "error_stack")
    private String errorStack;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public String getExecutionId() {
        return executionId;
    }

    public String getJobId() {
        return jobId;
    }

    public Instant getScheduledTime() {
        return scheduledTime;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public SchedulerExecutionResult getResult() {
        return result;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public SchedulerTriggerType getTriggerType() {
        return triggerType;
    }

    public String getResultCode() {
        return resultCode;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorStack() {
        return errorStack;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public static SchedulerJobExecution start(String executionId, String jobId, Instant scheduledTime, Instant startTime,
            SchedulerTriggerType triggerType) {
        SchedulerJobExecution execution = new SchedulerJobExecution();
        execution.executionId = executionId;
        execution.jobId = jobId;
        execution.scheduledTime = scheduledTime;
        execution.startTime = startTime;
        execution.triggerType = triggerType;
        execution.createdAt = HighDate.mockInstant();
        return execution;
    }

    public void finishSuccess(Instant endTime, long durationMs, JobResult result) {
        this.endTime = endTime;
        this.durationMs = durationMs;
        this.result = SchedulerExecutionResult.SUCCESS;
        this.resultCode = result == null ? null : result.code();
        this.resultMessage = result == null ? null : result.message();
    }

    public void finishFailure(Instant endTime, long durationMs, String errorMessage, String errorStack) {
        this.endTime = endTime;
        this.durationMs = durationMs;
        this.result = SchedulerExecutionResult.FAILED;
        this.errorMessage = errorMessage;
        this.errorStack = errorStack;
    }

    public void finishSkipped(Instant endTime, String message) {
        this.endTime = endTime;
        this.durationMs = 0L;
        this.result = SchedulerExecutionResult.SKIPPED;
        this.resultMessage = message;
    }
}

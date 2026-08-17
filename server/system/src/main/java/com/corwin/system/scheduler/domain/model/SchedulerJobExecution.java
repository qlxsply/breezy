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
 * JPA entity representing the execution history of a scheduled job.
 *
 * @author Corwin 2026/4/15
 */
@Entity
@Table(name = "sched_job_execution")
public class SchedulerJobExecution {

    /** Unique execution identifier. */
    @Id
    @Column(name = "execution_id", length = 64)
    private String executionId;

    /** Foreign key to the job definition. */
    @Column(name = "job_id", nullable = false, length = 128)
    private String jobId;

    /** The time the execution was scheduled to run. */
    @Column(name = "scheduled_time")
    private Instant scheduledTime;

    /** Actual start time of the execution. */
    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    /** Actual end time of the execution. */
    @Column(name = "end_time")
    private Instant endTime;

    /** Overall execution result (SUCCESS, FAILED, SKIPPED, etc.). */
    @Enumerated(EnumType.STRING)
    @Column(name = "execution_result", length = 32)
    private SchedulerExecutionResult result;

    /** Execution duration in milliseconds. */
    @Column(name = "duration_ms")
    private Long durationMs;

    /** How this execution was triggered. */
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 32)
    private SchedulerTriggerType triggerType;

    /** Optional result code returned by the handler. */
    @Column(name = "result_code", length = 64)
    private String resultCode;

    /** Optional result message returned by the handler. */
    @Column(name = "result_message", length = 2000)
    private String resultMessage;

    /** Error message if the execution failed. */
    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    /** Full stack trace if the execution failed. */
    @Lob
    @Column(name = "error_stack")
    private String errorStack;

    /** Timestamp when this execution record was created. */
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

    /**
     * Creates a new execution record with STARTED state.
     *
     * @param executionId   unique execution identifier
     * @param jobId         the job ID
     * @param scheduledTime the scheduled fire time
     * @param startTime     actual start time
     * @param triggerType   how the execution was triggered
     * @return a new SchedulerJobExecution
     */
    public static SchedulerJobExecution start(String executionId, String jobId, Instant scheduledTime, Instant startTime,
            SchedulerTriggerType triggerType) {
        SchedulerJobExecution execution = new SchedulerJobExecution();
        execution.executionId = executionId;
        execution.jobId = jobId;
        execution.scheduledTime = scheduledTime;
        execution.startTime = startTime;
        execution.triggerType = triggerType;
        execution.createdAt = HighDate.realInstant();
        return execution;
    }

    /** Marks this execution as successfully completed. */
    public void finishSuccess(Instant endTime, long durationMs, JobResult result) {
        this.endTime = endTime;
        this.durationMs = durationMs;
        this.result = SchedulerExecutionResult.SUCCESS;
        this.resultCode = result == null ? null : result.code();
        this.resultMessage = result == null ? null : result.message();
    }

    /** Marks this execution as failed with error details. */
    public void finishFailure(Instant endTime, long durationMs, String errorMessage, String errorStack) {
        this.endTime = endTime;
        this.durationMs = durationMs;
        this.result = SchedulerExecutionResult.FAILED;
        this.errorMessage = errorMessage;
        this.errorStack = errorStack;
    }

    /** Marks this execution as skipped (e.g. due to concurrent execution). */
    public void finishSkipped(Instant endTime, String message) {
        this.endTime = endTime;
        this.durationMs = 0L;
        this.result = SchedulerExecutionResult.SKIPPED;
        this.resultMessage = message;
    }
}

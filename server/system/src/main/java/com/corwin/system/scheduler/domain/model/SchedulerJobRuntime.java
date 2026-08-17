package com.corwin.system.scheduler.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * JPA entity representing the runtime state of a scheduled job.
 *
 * @author Corwin 2026/4/15
 */
@Entity
@Table(name = "sched_job_runtime")
public class SchedulerJobRuntime {

    /** Unique job identifier (matches SchedulerJobDefinition.jobId). */
    @Id
    @Column(name = "job_id", length = 128)
    private String jobId;

    /** Current runtime status of the job. */
    @Enumerated(EnumType.STRING)
    @Column(name = "job_status", nullable = false, length = 32)
    private SchedulerJobStatus status;

    /** Next scheduled fire time, or null if not scheduled. */
    @Column(name = "next_fire_time")
    private Instant nextFireTime;

    /** Most recent fire time. */
    @Column(name = "last_fire_time")
    private Instant lastFireTime;

    /** Most recent successful completion time. */
    @Column(name = "last_success_time")
    private Instant lastSuccessTime;

    /** Most recent failure time. */
    @Column(name = "last_failure_time")
    private Instant lastFailureTime;

    /** Number of consecutive failures since last success. */
    @Column(name = "consecutive_failures", nullable = false)
    private Long consecutiveFailures;

    /** Error message from the most recent failure. */
    @Column(name = "last_error_message", length = 2000)
    private String lastErrorMessage;

    /** Duration in milliseconds of the most recent execution. */
    @Column(name = "last_duration_ms")
    private Long lastDurationMs;

    /** Execution ID of the currently running execution, or null. */
    @Column(name = "current_execution_id", length = 64)
    private String currentExecutionId;

    /** Timestamp of the last update to this record. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getJobId() {
        return jobId;
    }

    public SchedulerJobStatus getStatus() {
        return status;
    }

    public Instant getNextFireTime() {
        return nextFireTime;
    }

    public Instant getLastFireTime() {
        return lastFireTime;
    }

    public Instant getLastSuccessTime() {
        return lastSuccessTime;
    }

    public Instant getLastFailureTime() {
        return lastFailureTime;
    }

    public Long getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public Long getLastDurationMs() {
        return lastDurationMs;
    }

    public String getCurrentExecutionId() {
        return currentExecutionId;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Creates a new runtime state record.
     *
     * @param jobId       the job ID
     * @param status      initial status
     * @param nextFireTime the next scheduled fire time
     * @return a new SchedulerJobRuntime
     */
    public static SchedulerJobRuntime create(String jobId, SchedulerJobStatus status, Instant nextFireTime) {
        SchedulerJobRuntime runtime = new SchedulerJobRuntime();
        runtime.jobId = jobId;
        runtime.status = status;
        runtime.nextFireTime = nextFireTime;
        runtime.consecutiveFailures = 0L;
        runtime.updatedAt = HighDate.realInstant();
        return runtime;
    }

    /** Transitions state to SCHEDULED with the given next fire time. */
    public void markScheduled(Instant nextFireTime) {
        this.status = SchedulerJobStatus.SCHEDULED;
        this.nextFireTime = nextFireTime;
        this.currentExecutionId = null;
        this.updatedAt = HighDate.realInstant();
    }

    /** Transitions state to PAUSED. */
    public void markPaused() {
        this.status = SchedulerJobStatus.PAUSED;
        this.nextFireTime = null;
        this.currentExecutionId = null;
        this.updatedAt = HighDate.realInstant();
    }

    /** Transitions state to CANCELLED. */
    public void markCancelled() {
        this.status = SchedulerJobStatus.CANCELLED;
        this.nextFireTime = null;
        this.currentExecutionId = null;
        this.updatedAt = HighDate.realInstant();
    }

    /** Transitions state to INVALID with an error message. */
    public void markInvalid(String message) {
        this.status = SchedulerJobStatus.INVALID;
        this.nextFireTime = null;
        this.lastErrorMessage = message;
        this.currentExecutionId = null;
        this.updatedAt = HighDate.realInstant();
    }

    /** Transitions state to ERROR with an error message. */
    public void markError(String message) {
        this.status = SchedulerJobStatus.ERROR;
        this.lastErrorMessage = message;
        this.currentExecutionId = null;
        this.updatedAt = HighDate.realInstant();
    }

    /** Transitions state to RUNNING with the current execution ID. */
    public void markRunning(String executionId, Instant scheduledTime) {
        this.status = SchedulerJobStatus.RUNNING;
        this.currentExecutionId = executionId;
        this.lastFireTime = scheduledTime;
        this.updatedAt = HighDate.realInstant();
    }

    /** Transitions state to SCHEDULED or CANCELLED after successful execution. */
    public void markSuccess(Instant nextFireTime, Instant endTime, long durationMs) {
        this.status = nextFireTime == null ? SchedulerJobStatus.CANCELLED : SchedulerJobStatus.SCHEDULED;
        this.nextFireTime = nextFireTime;
        this.lastSuccessTime = endTime;
        this.lastDurationMs = durationMs;
        this.lastErrorMessage = null;
        this.consecutiveFailures = 0L;
        this.currentExecutionId = null;
        this.updatedAt = HighDate.realInstant();
    }

    /** Transitions state to SCHEDULED or ERROR after a failed execution. */
    public void markFailure(Instant nextFireTime, Instant endTime, long durationMs, String errorMessage) {
        this.status = nextFireTime == null ? SchedulerJobStatus.ERROR : SchedulerJobStatus.SCHEDULED;
        this.nextFireTime = nextFireTime;
        this.lastFailureTime = endTime;
        this.lastDurationMs = durationMs;
        this.lastErrorMessage = errorMessage;
        this.consecutiveFailures = (this.consecutiveFailures == null ? 0L : this.consecutiveFailures) + 1L;
        this.currentExecutionId = null;
        this.updatedAt = HighDate.realInstant();
    }
}

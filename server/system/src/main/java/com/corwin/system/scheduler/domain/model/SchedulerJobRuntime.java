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
 * 任务运行态。
 *
 * @author Corwin 2026/4/15
 */
@Entity
@Table(name = "sched_job_runtime")
public class SchedulerJobRuntime {

    @Id
    @Column(name = "job_id", length = 128)
    private String jobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_status", nullable = false, length = 32)
    private SchedulerJobStatus status;

    @Column(name = "next_fire_time")
    private Instant nextFireTime;

    @Column(name = "last_fire_time")
    private Instant lastFireTime;

    @Column(name = "last_success_time")
    private Instant lastSuccessTime;

    @Column(name = "last_failure_time")
    private Instant lastFailureTime;

    @Column(name = "consecutive_failures", nullable = false)
    private Long consecutiveFailures;

    @Column(name = "last_error_message", length = 2000)
    private String lastErrorMessage;

    @Column(name = "last_duration_ms")
    private Long lastDurationMs;

    @Column(name = "current_execution_id", length = 64)
    private String currentExecutionId;

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

    public static SchedulerJobRuntime create(String jobId, SchedulerJobStatus status, Instant nextFireTime) {
        SchedulerJobRuntime runtime = new SchedulerJobRuntime();
        runtime.jobId = jobId;
        runtime.status = status;
        runtime.nextFireTime = nextFireTime;
        runtime.consecutiveFailures = 0L;
        runtime.updatedAt = HighDate.mockInstant();
        return runtime;
    }

    public void markScheduled(Instant nextFireTime) {
        this.status = SchedulerJobStatus.SCHEDULED;
        this.nextFireTime = nextFireTime;
        this.currentExecutionId = null;
        this.updatedAt = HighDate.mockInstant();
    }

    public void markPaused() {
        this.status = SchedulerJobStatus.PAUSED;
        this.nextFireTime = null;
        this.currentExecutionId = null;
        this.updatedAt = HighDate.mockInstant();
    }

    public void markCancelled() {
        this.status = SchedulerJobStatus.CANCELLED;
        this.nextFireTime = null;
        this.currentExecutionId = null;
        this.updatedAt = HighDate.mockInstant();
    }

    public void markInvalid(String message) {
        this.status = SchedulerJobStatus.INVALID;
        this.nextFireTime = null;
        this.lastErrorMessage = message;
        this.currentExecutionId = null;
        this.updatedAt = HighDate.mockInstant();
    }

    public void markError(String message) {
        this.status = SchedulerJobStatus.ERROR;
        this.lastErrorMessage = message;
        this.currentExecutionId = null;
        this.updatedAt = HighDate.mockInstant();
    }

    public void markRunning(String executionId, Instant scheduledTime) {
        this.status = SchedulerJobStatus.RUNNING;
        this.currentExecutionId = executionId;
        this.lastFireTime = scheduledTime;
        this.updatedAt = HighDate.mockInstant();
    }

    public void markSuccess(Instant nextFireTime, Instant endTime, long durationMs) {
        this.status = nextFireTime == null ? SchedulerJobStatus.CANCELLED : SchedulerJobStatus.SCHEDULED;
        this.nextFireTime = nextFireTime;
        this.lastSuccessTime = endTime;
        this.lastDurationMs = durationMs;
        this.lastErrorMessage = null;
        this.consecutiveFailures = 0L;
        this.currentExecutionId = null;
        this.updatedAt = HighDate.mockInstant();
    }

    public void markFailure(Instant nextFireTime, Instant endTime, long durationMs, String errorMessage) {
        this.status = nextFireTime == null ? SchedulerJobStatus.ERROR : SchedulerJobStatus.SCHEDULED;
        this.nextFireTime = nextFireTime;
        this.lastFailureTime = endTime;
        this.lastDurationMs = durationMs;
        this.lastErrorMessage = errorMessage;
        this.consecutiveFailures = (this.consecutiveFailures == null ? 0L : this.consecutiveFailures) + 1L;
        this.currentExecutionId = null;
        this.updatedAt = HighDate.mockInstant();
    }
}

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
 * JPA entity representing the definition of a scheduled job.
 *
 * @author Corwin 2026/4/15
 */
@Entity
@Table(name = "sched_job")
public class SchedulerJobDefinition {

    /** Unique job identifier. */
    @Id
    @Column(name = "job_id", length = 128)
    private String jobId;

    /** Namespace used for job grouping and isolation. */
    @Column(name = "namespace", nullable = false, length = 64)
    private String namespace;

    /** Human-readable job display name. */
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    /** Source of the job definition (ANNOTATION or EVENT). */
    @Enumerated(EnumType.STRING)
    @Column(name = "job_source", nullable = false, length = 32)
    private SchedulerJobSource source;

    /** Type of job execution (METHOD or HANDLER). */
    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 32)
    private SchedulerJobType jobType;

    /** Key referencing the registered handler implementation. */
    @Column(name = "handler_key", length = 128)
    private String handlerKey;

    /** Fully qualified class name of the schedule rule implementation. */
    @Column(name = "schedule_rule_type", nullable = false, length = 128)
    private String scheduleRuleType;

    /** Serialized schedule rule body in JSON format. */
    @Lob
    @Column(name = "schedule_rule_body")
    private byte[] scheduleRuleBody;

    /** Fully qualified class name of the job payload. */
    @Column(name = "payload_type", length = 256)
    private String payloadType;

    /** Serialized job payload in JSON format. */
    @Lob
    @Column(name = "payload_body")
    private byte[] payloadBody;

    /** Whether the job is enabled for execution. */
    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    /** Soft-delete flag. */
    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    /** Whether concurrent executions are allowed. */
    @Column(name = "allow_concurrent", nullable = false)
    private boolean allowConcurrent;

    /** Optimistic locking version number. */
    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    /** Optional remark or description. */
    @Column(name = "remark", length = 512)
    private String remark;

    /** Timestamp when this definition was created. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Timestamp when this definition was last updated. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getJobId() {
        return jobId;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getName() {
        return name;
    }

    public SchedulerJobSource getSource() {
        return source;
    }

    public SchedulerJobType getJobType() {
        return jobType;
    }

    public String getHandlerKey() {
        return handlerKey;
    }

    public String getScheduleRuleType() {
        return scheduleRuleType;
    }

    public byte[] getScheduleRuleBody() {
        return scheduleRuleBody;
    }

    public String getPayloadType() {
        return payloadType;
    }

    public byte[] getPayloadBody() {
        return payloadBody;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isAllowConcurrent() {
        return allowConcurrent;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public String getRemark() {
        return remark;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Creates a new EVENT-sourced job definition.
     *
     * @param jobId            unique job identifier
     * @param namespace        job namespace
     * @param name             job display name
     * @param handlerKey       handler key reference
     * @param scheduleRule     scheduling rule
     * @param scheduleRuleBody serialized schedule rule bytes
     * @param payloadType      optional payload type name
     * @param payloadBody      serialized payload bytes
     * @param enabled          initial enabled state
     * @param allowConcurrent  whether concurrent execution is allowed
     * @param remark           optional remark
     * @return a new SchedulerJobDefinition
     */
    public static SchedulerJobDefinition createEventJob(String jobId, String namespace, String name, HandlerKey handlerKey,
            ScheduleRule scheduleRule, byte[] scheduleRuleBody, String payloadType, byte[] payloadBody, boolean enabled,
            boolean allowConcurrent, String remark) {
        SchedulerJobDefinition definition = new SchedulerJobDefinition();
        definition.jobId = jobId;
        definition.namespace = namespace;
        definition.name = name;
        definition.source = SchedulerJobSource.EVENT;
        definition.jobType = SchedulerJobType.HANDLER;
        definition.handlerKey = handlerKey == null ? null : handlerKey.value();
        definition.scheduleRuleType = scheduleRule == null ? null : scheduleRule.getClass().getName();
        definition.scheduleRuleBody = scheduleRuleBody;
        definition.payloadType = payloadType;
        definition.payloadBody = payloadBody;
        definition.enabled = enabled;
        definition.deleted = false;
        definition.allowConcurrent = allowConcurrent;
        definition.versionNo = 1;
        definition.remark = remark;
        definition.createdAt = HighDate.mockInstant();
        definition.updatedAt = definition.createdAt;
        return definition;
    }

    /**
     * Updates an existing event job definition with new values.
     */
    public void updateEventJob(String namespace, String name, HandlerKey handlerKey, ScheduleRule scheduleRule,
            byte[] scheduleRuleBody, String payloadType, byte[] payloadBody, boolean enabled, boolean allowConcurrent,
            String remark) {
        this.namespace = namespace;
        this.name = name;
        this.handlerKey = handlerKey == null ? null : handlerKey.value();
        this.scheduleRuleType = scheduleRule == null ? null : scheduleRule.getClass().getName();
        this.scheduleRuleBody = scheduleRuleBody;
        this.payloadType = payloadType;
        this.payloadBody = payloadBody;
        this.enabled = enabled;
        this.allowConcurrent = allowConcurrent;
        this.remark = remark;
        this.deleted = false;
        this.versionNo = (this.versionNo == null ? 1 : this.versionNo + 1);
        touch();
    }

    /** Marks this job definition as paused (disabled). */
    public void markPaused() {
        this.enabled = false;
        touch();
    }

    /** Marks this job definition as resumed (enabled and not deleted). */
    public void markResumed() {
        this.enabled = true;
        this.deleted = false;
        touch();
    }

    /** Marks this job definition as cancelled (disabled). */
    public void markCancelled() {
        this.enabled = false;
        touch();
    }

    /** Marks this job definition as deleted (disabled and soft-deleted). */
    public void markDeleted() {
        this.enabled = false;
        this.deleted = true;
        touch();
    }

    private void touch() {
        this.updatedAt = HighDate.mockInstant();
    }
}

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
 * 动态任务定义。
 *
 * @author Corwin 2026/4/15
 */
@Entity
@Table(name = "sched_job")
public class SchedulerJobDefinition {

    @Id
    @Column(name = "job_id", length = 128)
    private String jobId;

    @Column(name = "namespace", nullable = false, length = 64)
    private String namespace;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_source", nullable = false, length = 32)
    private SchedulerJobSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 32)
    private SchedulerJobType jobType;

    @Column(name = "handler_key", length = 128)
    private String handlerKey;

    @Column(name = "schedule_rule_type", nullable = false, length = 128)
    private String scheduleRuleType;

    @Lob
    @Column(name = "schedule_rule_body")
    private byte[] scheduleRuleBody;

    @Column(name = "payload_type", length = 256)
    private String payloadType;

    @Lob
    @Column(name = "payload_body")
    private byte[] payloadBody;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "allow_concurrent", nullable = false)
    private boolean allowConcurrent;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(name = "remark", length = 512)
    private String remark;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

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

    public void markPaused() {
        this.enabled = false;
        touch();
    }

    public void markResumed() {
        this.enabled = true;
        this.deleted = false;
        touch();
    }

    public void markCancelled() {
        this.enabled = false;
        touch();
    }

    public void markDeleted() {
        this.enabled = false;
        this.deleted = true;
        touch();
    }

    private void touch() {
        this.updatedAt = HighDate.mockInstant();
    }
}

package com.corwin.system.normalfeature.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * 用户功能用户特例
 *
 * @author Corwin 2026/4/19
 */
@Getter
@Entity
@Table(name = "sys_normal_feature_user_override", indexes = {
        @Index(name = "idx_sys_normal_feature_user_override_unique", columnList = "user_id,feature_id", unique = true),
        @Index(name = "idx_sys_normal_feature_user_override_user_id", columnList = "user_id"),
        @Index(name = "idx_sys_normal_feature_user_override_feature_id", columnList = "feature_id")})
public class NormalFeatureUserOverride {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "feature_id", nullable = false)
    private Long featureId;

    @Enumerated(EnumType.STRING)
    @Column(name = "override_type", nullable = false, length = 16)
    private NormalFeatureOverrideType overrideType;

    @Column(name = "reason", length = 512)
    private String reason;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NormalFeatureUserOverride() {
    }

    public NormalFeatureUserOverride(Long userId, Long featureId, NormalFeatureOverrideType overrideType, String reason,
            Long operator) {
        Instant now = HighDate.mockInstant();
        this.userId = userId;
        this.featureId = featureId;
        this.overrideType = overrideType;
        this.reason = reason;
        this.createdBy = operator;
        this.createdAt = now;
        this.updatedBy = operator;
        this.updatedAt = now;
    }

    public boolean enable() {
        return overrideType == NormalFeatureOverrideType.ENABLE;
    }

    public boolean disable() {
        return overrideType == NormalFeatureOverrideType.DISABLE;
    }

    public void changeToEnable(String reason, Long operator) {
        this.overrideType = NormalFeatureOverrideType.ENABLE;
        this.reason = reason;
        touch(operator);
    }

    public void changeToDisable(String reason, Long operator) {
        this.overrideType = NormalFeatureOverrideType.DISABLE;
        this.reason = reason;
        touch(operator);
    }

    public void clear(String reason, Long operator) {
        this.overrideType = NormalFeatureOverrideType.NONE;
        this.reason = reason;
        touch(operator);
    }

    private void touch(Long operator) {
        this.updatedBy = operator;
        this.updatedAt = HighDate.mockInstant();
    }
}

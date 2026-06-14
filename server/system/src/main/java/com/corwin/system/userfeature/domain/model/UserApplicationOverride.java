package com.corwin.system.userfeature.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * 用户应用访问特例。
 *
 * <p>表示单个用户对某个产品应用的访问特例，并定义单独启用时覆盖完整功能或部分功能。</p>
 *
 * @author Corwin
 */
@Getter
@Entity
@Table(name = "sys_user_application_override", indexes = {
        @Index(name = "idx_sys_user_application_override_unique", columnList = "user_id,application_id", unique = true),
        @Index(name = "idx_sys_user_application_override_user_id", columnList = "user_id"),
        @Index(name = "idx_sys_user_application_override_application_id", columnList = "application_id"),
        @Index(name = "idx_sys_user_application_override_type", columnList = "override_type"),
        @Index(name = "idx_sys_user_application_override_scope", columnList = "feature_access_scope")})
public class UserApplicationOverride {

    /**
     * 主键 ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户 ID。
     *
     * <p>逻辑引用用户表主键，数据库层面不创建外键。</p>
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 产品应用 ID。
     *
     * <p>逻辑引用 {@link ProductApplication#getId()}，数据库层面不创建外键。</p>
     */
    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    /**
     * 用户应用访问特例类型。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "override_type", nullable = false, length = 16)
    private UserAccessOverrideType overrideType;

    /**
     * 应用功能访问范围。
     *
     * <p>
     * 当 overrideType 为 ENABLE 时生效。
     * FULL 表示用户拥有应用下全部已启用功能，后续新增功能自动可用。
     * PARTIAL 表示用户仅拥有显式开启的功能，后续新增功能默认不可用。
     * </p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "feature_access_scope", nullable = false, length = 16)
    private ApplicationFeatureAccessScope featureAccessScope;

    /**
     * 特例原因。
     */
    @Column(name = "reason", length = 512)
    private String reason;

    /**
     * 创建人用户 ID。
     */
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * 创建时间。
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * 最后更新人用户 ID。
     */
    @Column(name = "updated_by")
    private Long updatedBy;

    /**
     * 最后更新时间。
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * JPA 构造方法。
     */
    protected UserApplicationOverride() {
    }

    /**
     * 创建用户应用访问特例。
     *
     * @param userId             用户 ID
     * @param applicationId      产品应用 ID
     * @param overrideType       特例类型
     * @param featureAccessScope 应用功能访问范围
     * @param reason             特例原因
     * @param operator           操作人用户 ID
     */
    public UserApplicationOverride(Long userId, Long applicationId, UserAccessOverrideType overrideType,
            ApplicationFeatureAccessScope featureAccessScope, String reason, Long operator) {
        Instant now = HighDate.mockInstant();
        this.userId = userId;
        this.applicationId = applicationId;
        this.overrideType = overrideType;
        this.featureAccessScope = featureAccessScope;
        this.reason = reason;
        this.createdBy = operator;
        this.createdAt = now;
        this.updatedBy = operator;
        this.updatedAt = now;
    }

    /**
     * 单独启用应用完整功能。
     *
     * @param reason   特例原因
     * @param operator 操作人用户 ID
     */
    public void changeToEnableFullAccess(String reason, Long operator) {
        this.overrideType = UserAccessOverrideType.ENABLE;
        this.featureAccessScope = ApplicationFeatureAccessScope.FULL;
        this.reason = reason;
        touch(operator);
    }

    /**
     * 单独启用应用部分功能。
     *
     * @param reason   特例原因
     * @param operator 操作人用户 ID
     */
    public void changeToEnablePartialAccess(String reason, Long operator) {
        this.overrideType = UserAccessOverrideType.ENABLE;
        this.featureAccessScope = ApplicationFeatureAccessScope.PARTIAL;
        this.reason = reason;
        touch(operator);
    }

    /**
     * 单独禁用应用。
     *
     * @param reason   特例原因
     * @param operator 操作人用户 ID
     */
    public void changeToDisable(String reason, Long operator) {
        this.overrideType = UserAccessOverrideType.DISABLE;
        this.featureAccessScope = ApplicationFeatureAccessScope.PARTIAL;
        this.reason = reason;
        touch(operator);
    }

    /**
     * 清除应用访问特例。
     *
     * @param reason   清除原因
     * @param operator 操作人用户 ID
     */
    public void clear(String reason, Long operator) {
        this.overrideType = UserAccessOverrideType.NONE;
        this.featureAccessScope = ApplicationFeatureAccessScope.PARTIAL;
        this.reason = reason;
        touch(operator);
    }

    /**
     * 判断是否为单独启用。
     *
     * @return true 表示单独启用
     */
    public boolean enable() {
        return overrideType == UserAccessOverrideType.ENABLE;
    }

    /**
     * 判断是否为单独禁用。
     *
     * @return true 表示单独禁用
     */
    public boolean disable() {
        return overrideType == UserAccessOverrideType.DISABLE;
    }

    /**
     * 判断是否为完整功能访问。
     *
     * @return true 表示完整功能访问
     */
    public boolean fullAccess() {
        return featureAccessScope == ApplicationFeatureAccessScope.FULL;
    }

    /**
     * 判断是否为部分功能访问。
     *
     * @return true 表示部分功能访问
     */
    public boolean partialAccess() {
        return featureAccessScope == ApplicationFeatureAccessScope.PARTIAL;
    }

    /**
     * 刷新审计字段。
     *
     * @param operator 操作人用户 ID
     */
    private void touch(Long operator) {
        this.updatedBy = operator;
        this.updatedAt = HighDate.mockInstant();
    }
}

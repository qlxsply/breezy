package com.corwin.system.userfeature.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;

/**
 * 用户功能访问特例。
 *
 * <p>表示单个用户对产品应用内某个功能的单独启用、单独禁用或清除特例。
 *
 * @author Corwin
 */
@Getter
@Entity
@Table(
    name = "sys_user_feature_override",
    indexes = {
      @Index(
          name = "idx_sys_user_feature_override_unique",
          columnList = "user_id,feature_id",
          unique = true),
      @Index(name = "idx_sys_user_feature_override_user_id", columnList = "user_id"),
      @Index(name = "idx_sys_user_feature_override_application_id", columnList = "application_id"),
      @Index(name = "idx_sys_user_feature_override_feature_id", columnList = "feature_id"),
      @Index(name = "idx_sys_user_feature_override_type", columnList = "override_type")
    })
public class UserFeatureOverride {

  /** 主键 ID。 */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * 用户 ID。
   *
   * <p>逻辑引用用户表主键，数据库层面不创建外键。
   */
  @Column(name = "user_id", nullable = false)
  private Long userId;

  /**
   * 产品应用 ID。
   *
   * <p>逻辑引用 {@link ProductApplication#getId()}，用于查询和一致性校验。
   */
  @Column(name = "application_id", nullable = false)
  private Long applicationId;

  /**
   * 产品功能 ID。
   *
   * <p>逻辑引用 {@link ProductFeature#getId()}，数据库层面不创建外键。
   */
  @Column(name = "feature_id", nullable = false)
  private Long featureId;

  /** 用户功能访问特例类型。 */
  @Enumerated(EnumType.STRING)
  @Column(name = "override_type", nullable = false, length = 16)
  private UserAccessOverrideType overrideType;

  /** 特例原因。 */
  @Column(name = "reason", length = 512)
  private String reason;

  /** 创建人用户 ID。 */
  @Column(name = "created_by")
  private Long createdBy;

  /** 创建时间。 */
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** 最后更新人用户 ID。 */
  @Column(name = "updated_by")
  private Long updatedBy;

  /** 最后更新时间。 */
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** JPA 构造方法。 */
  protected UserFeatureOverride() {}

  /**
   * 创建用户功能访问特例。
   *
   * @param userId 用户 ID
   * @param applicationId 产品应用 ID
   * @param featureId 产品功能 ID
   * @param overrideType 特例类型
   * @param reason 特例原因
   * @param operator 操作人用户 ID
   */
  public UserFeatureOverride(
      Long userId,
      Long applicationId,
      Long featureId,
      UserAccessOverrideType overrideType,
      String reason,
      Long operator) {
    Instant now = HighDate.mockInstant();
    this.userId = userId;
    this.applicationId = applicationId;
    this.featureId = featureId;
    this.overrideType = overrideType;
    this.reason = reason;
    this.createdBy = operator;
    this.createdAt = now;
    this.updatedBy = operator;
    this.updatedAt = now;
  }

  /**
   * 单独启用功能。
   *
   * @param reason 特例原因
   * @param operator 操作人用户 ID
   */
  public void changeToEnable(String reason, Long operator) {
    this.overrideType = UserAccessOverrideType.ENABLE;
    this.reason = reason;
    touch(operator);
  }

  /**
   * 单独禁用功能。
   *
   * @param reason 特例原因
   * @param operator 操作人用户 ID
   */
  public void changeToDisable(String reason, Long operator) {
    this.overrideType = UserAccessOverrideType.DISABLE;
    this.reason = reason;
    touch(operator);
  }

  /**
   * 清除功能访问特例。
   *
   * @param reason 清除原因
   * @param operator 操作人用户 ID
   */
  public void clear(String reason, Long operator) {
    this.overrideType = UserAccessOverrideType.NONE;
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
   * 刷新审计字段。
   *
   * @param operator 操作人用户 ID
   */
  private void touch(Long operator) {
    this.updatedBy = operator;
    this.updatedAt = HighDate.mockInstant();
  }
}

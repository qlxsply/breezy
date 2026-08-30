package com.corwin.system.userfeature.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;

/**
 * 用户包功能授权。
 *
 * <p>表示用户应用包在部分功能访问模式下可使用的应用内功能。
 *
 * @author Corwin
 */
@Getter
@Entity
@Table(
    name = "sys_user_package_feature_access",
    indexes = {
      @Index(
          name = "idx_sys_user_package_feature_access_unique",
          columnList = "package_id,feature_id",
          unique = true),
      @Index(name = "idx_sys_user_package_feature_access_package_id", columnList = "package_id"),
      @Index(
          name = "idx_sys_user_package_feature_access_application_id",
          columnList = "application_id"),
      @Index(name = "idx_sys_user_package_feature_access_feature_id", columnList = "feature_id")
    })
public class UserPackageFeatureAccess {

  /** 主键 ID。 */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * 用户应用包 ID。
   *
   * <p>逻辑引用 {@link UserApplicationPackage#getId()}，数据库层面不创建外键。
   */
  @Column(name = "package_id", nullable = false)
  private Long packageId;

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

  /** 是否系统内置。 */
  @Column(name = "system_built_in", nullable = false)
  private Boolean systemBuiltIn;

  /** 创建人用户 ID。 */
  @Column(name = "created_by")
  private Long createdBy;

  /** 创建时间。 */
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** JPA 构造方法。 */
  protected UserPackageFeatureAccess() {}

  /**
   * 创建用户包功能授权。
   *
   * @param packageId 用户应用包 ID
   * @param applicationId 产品应用 ID
   * @param featureId 产品功能 ID
   * @param systemBuiltIn 是否系统内置
   * @param operator 操作人用户 ID
   */
  public UserPackageFeatureAccess(
      Long packageId, Long applicationId, Long featureId, Boolean systemBuiltIn, Long operator) {
    this.packageId = packageId;
    this.applicationId = applicationId;
    this.featureId = featureId;
    this.systemBuiltIn = systemBuiltIn;
    this.createdBy = operator;
    this.createdAt = HighDate.mockInstant();
  }
}

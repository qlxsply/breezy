package com.corwin.system.userfeature.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;

/**
 * 用户包应用授权。
 *
 * <p>表示用户应用包对产品应用的访问授权，并定义该授权覆盖完整功能或部分功能。
 *
 * @author Corwin
 */
@Getter
@Entity
@Table(
    name = "sys_user_package_application_access",
    indexes = {
      @Index(
          name = "idx_sys_user_package_application_access_unique",
          columnList = "package_id,application_id",
          unique = true),
      @Index(
          name = "idx_sys_user_package_application_access_package_id",
          columnList = "package_id"),
      @Index(
          name = "idx_sys_user_package_application_access_application_id",
          columnList = "application_id"),
      @Index(
          name = "idx_sys_user_package_application_access_scope",
          columnList = "feature_access_scope")
    })
public class UserPackageApplicationAccess {

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
   * <p>逻辑引用 {@link ProductApplication#getId()}，数据库层面不创建外键。
   */
  @Column(name = "application_id", nullable = false)
  private Long applicationId;

  /**
   * 应用功能访问范围。
   *
   * <p>FULL 表示拥有应用下全部已启用功能，后续新增功能自动可用。 PARTIAL 表示仅拥有显式授权功能，后续新增功能默认不可用。
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "feature_access_scope", nullable = false, length = 16)
  private ApplicationFeatureAccessScope featureAccessScope;

  /** 是否系统内置。 */
  @Column(name = "system_built_in", nullable = false)
  private Boolean systemBuiltIn;

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
  protected UserPackageApplicationAccess() {}

  /**
   * 创建用户包应用授权。
   *
   * @param packageId 用户应用包 ID
   * @param applicationId 产品应用 ID
   * @param featureAccessScope 应用功能访问范围
   * @param systemBuiltIn 是否系统内置
   * @param operator 操作人用户 ID
   */
  public UserPackageApplicationAccess(
      Long packageId,
      Long applicationId,
      ApplicationFeatureAccessScope featureAccessScope,
      Boolean systemBuiltIn,
      Long operator) {
    Instant now = HighDate.mockInstant();
    this.packageId = packageId;
    this.applicationId = applicationId;
    this.featureAccessScope = featureAccessScope;
    this.systemBuiltIn = systemBuiltIn;
    this.createdBy = operator;
    this.createdAt = now;
    this.updatedBy = operator;
    this.updatedAt = now;
  }

  /**
   * 设置为完整功能访问。
   *
   * <p>设置为 FULL 后，业务层不应再维护该包在当前应用下的功能授权明细。
   *
   * @param operator 操作人用户 ID
   */
  public void changeToFullAccess(Long operator) {
    this.featureAccessScope = ApplicationFeatureAccessScope.FULL;
    touch(operator);
  }

  /**
   * 设置为部分功能访问。
   *
   * <p>设置为 PARTIAL 后，业务层应通过 {@link UserPackageFeatureAccess} 维护可用功能明细。
   *
   * @param operator 操作人用户 ID
   */
  public void changeToPartialAccess(Long operator) {
    this.featureAccessScope = ApplicationFeatureAccessScope.PARTIAL;
    touch(operator);
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

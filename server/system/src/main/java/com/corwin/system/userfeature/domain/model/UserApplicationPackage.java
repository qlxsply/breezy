package com.corwin.system.userfeature.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;

/**
 * 用户应用包。
 *
 * <p>表示一组用户共享的应用权益集合，例如普通用户、VIP1、VIP2、企业版。
 *
 * @author Corwin
 */
@Getter
@Entity
@Table(
    name = "sys_user_application_package",
    indexes = {
      @Index(
          name = "idx_sys_user_application_package_code",
          columnList = "package_code",
          unique = true),
      @Index(name = "idx_sys_user_application_package_type", columnList = "package_type"),
      @Index(name = "idx_sys_user_application_package_enabled", columnList = "enabled"),
      @Index(name = "idx_sys_user_application_package_default", columnList = "default_package")
    })
public class UserApplicationPackage {

  /** 主键 ID。 */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 用户应用包编码。 */
  @Column(name = "package_code", nullable = false, length = 128)
  private String packageCode;

  /** 用户应用包名称。 */
  @Column(name = "package_name", nullable = false, length = 128)
  private String packageName;

  /** 用户应用包类型。 */
  @Enumerated(EnumType.STRING)
  @Column(name = "package_type", nullable = false, length = 32)
  private UserApplicationPackageType packageType;

  /** 用户应用包描述。 */
  @Column(name = "description", length = 512)
  private String description;

  /**
   * 是否启用。
   *
   * <p>用户应用包关闭后，该包下应用授权和功能授权均不生效。
   */
  @Column(name = "enabled", nullable = false)
  private Boolean enabled;

  /** 是否默认用户应用包。 */
  @Column(name = "default_package", nullable = false)
  private Boolean defaultPackage;

  /** 是否系统内置。 */
  @Column(name = "system_built_in", nullable = false)
  private Boolean systemBuiltIn;

  /** 展示排序值。 */
  @Column(name = "display_order", nullable = false)
  private Integer displayOrder;

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
  protected UserApplicationPackage() {}

  /**
   * 创建用户应用包。
   *
   * @param packageCode 用户应用包编码
   * @param packageName 用户应用包名称
   * @param packageType 用户应用包类型
   * @param description 用户应用包描述
   * @param defaultPackage 是否默认用户应用包
   * @param systemBuiltIn 是否系统内置
   * @param displayOrder 展示排序值
   * @param operator 操作人用户 ID
   */
  public UserApplicationPackage(
      String packageCode,
      String packageName,
      UserApplicationPackageType packageType,
      String description,
      Boolean defaultPackage,
      Boolean systemBuiltIn,
      Integer displayOrder,
      Long operator) {
    Instant now = HighDate.mockInstant();
    this.packageCode = packageCode;
    this.packageName = packageName;
    this.packageType = packageType;
    this.description = description;
    this.enabled = true;
    this.defaultPackage = defaultPackage;
    this.systemBuiltIn = systemBuiltIn;
    this.displayOrder = displayOrder;
    this.createdBy = operator;
    this.createdAt = now;
    this.updatedBy = operator;
    this.updatedAt = now;
  }

  /**
   * 更新用户应用包基础信息。
   *
   * @param packageCode 用户应用包编码
   * @param packageName 用户应用包名称
   * @param packageType 用户应用包类型
   * @param description 用户应用包描述
   * @param defaultPackage 是否默认用户应用包
   * @param displayOrder 展示排序值
   * @param operator 操作人用户 ID
   */
  public void update(
      String packageCode,
      String packageName,
      UserApplicationPackageType packageType,
      String description,
      Boolean defaultPackage,
      Integer displayOrder,
      Long operator) {
    this.packageCode = packageCode;
    this.packageName = packageName;
    this.packageType = packageType;
    this.description = description;
    this.defaultPackage = defaultPackage;
    this.displayOrder = displayOrder;
    touch(operator);
  }

  /**
   * 启用用户应用包。
   *
   * @param operator 操作人用户 ID
   */
  public void enable(Long operator) {
    this.enabled = true;
    touch(operator);
  }

  /**
   * 关闭用户应用包。
   *
   * @param operator 操作人用户 ID
   */
  public void disable(Long operator) {
    this.enabled = false;
    touch(operator);
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

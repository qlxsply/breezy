package com.corwin.system.role.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;

/**
 * JPA entity representing a system role with code, name, enabled status and audit fields.
 *
 * @author Corwin 2026/1/23
 */
@Getter
@Entity
@Table(
    name = "sys_role",
    indexes = {
      @Index(name = "idx_sys_role_code", columnList = "code", unique = true),
      @Index(name = "idx_sys_role_enabled", columnList = "enabled")
    })
public class Role {

  /** Primary key. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Unique role code used for identification. */
  @Column(name = "code", nullable = false, length = 128)
  private String code;

  /** Display name of the role. */
  @Column(name = "name", nullable = false, length = 128)
  private String name;

  /** Optional description of the role. */
  @Column(name = "description", length = 512)
  private String description;

  /** Whether the role is enabled. */
  @Column(name = "enabled", nullable = false)
  private Boolean enabled;

  /** Whether this is a system-builtin role that cannot be deleted. */
  @Column(name = "system_builtin", nullable = false)
  private Boolean systemBuiltin;

  /** ID of the operator who created this role. */
  @Column(name = "created_by")
  private Long createdBy;

  /** Timestamp when this role was created. */
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** ID of the operator who last updated this role. */
  @Column(name = "updated_by")
  private Long updatedBy;

  /** Timestamp when this role was last updated. */
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Role() {}

  /**
   * Creates a new role with the given attributes. Defaults to enabled.
   *
   * @param code unique role code
   * @param name display name
   * @param description optional description
   * @param systemBuiltin whether this is a system-builtin role
   * @param operator ID of the creating operator
   */
  public Role(String code, String name, String description, Boolean systemBuiltin, Long operator) {
    Instant now = HighDate.mockInstant();
    this.code = code;
    this.name = name;
    this.description = description;
    this.enabled = true;
    this.systemBuiltin = systemBuiltin;
    this.createdBy = operator;
    this.createdAt = now;
    this.updatedBy = operator;
    this.updatedAt = now;
  }

  /**
   * Enables this role.
   *
   * @param operator ID of the operator
   */
  public void enable(Long operator) {
    this.enabled = true;
    touch(operator);
  }

  /**
   * Disables this role.
   *
   * @param operator ID of the operator
   */
  public void disable(Long operator) {
    this.enabled = false;
    touch(operator);
  }

  /**
   * Updates the code, name and description of this role.
   *
   * @param code new role code
   * @param name new display name
   * @param description new description
   * @param operator ID of the operator
   */
  public void update(String code, String name, String description, Long operator) {
    this.code = code;
    this.name = name;
    this.description = description;
    touch(operator);
  }

  public boolean isEnabled() {
    return Boolean.TRUE.equals(enabled);
  }

  private void touch(Long operator) {
    this.updatedBy = operator;
    this.updatedAt = HighDate.mockInstant();
  }
}

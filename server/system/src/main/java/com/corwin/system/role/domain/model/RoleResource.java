package com.corwin.system.role.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;

/**
 * JPA entity representing the many-to-many association between roles and resources.
 *
 * @author Corwin 2026/6/29
 */
@Getter
@Entity
@Table(
    name = "sys_role_resource",
    indexes = {
      @Index(
          name = "idx_sys_role_resource_unique",
          columnList = "role_id,resource_id",
          unique = true),
      @Index(name = "idx_sys_role_resource_role_id", columnList = "role_id"),
      @Index(name = "idx_sys_role_resource_resource_id", columnList = "resource_id")
    })
public class RoleResource {

  /** Primary key. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Foreign key to the associated role. */
  @Column(name = "role_id", nullable = false)
  private Long roleId;

  /** Foreign key to the associated resource. */
  @Column(name = "resource_id", nullable = false)
  private Long resourceId;

  /** ID of the operator who created this association. */
  @Column(name = "created_by")
  private Long createdBy;

  /** Timestamp when this association was created. */
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected RoleResource() {}

  /**
   * Creates a role-resource association.
   *
   * @param roleId the role ID
   * @param resourceId the resource ID
   * @param operator ID of the creating operator
   */
  public RoleResource(Long roleId, Long resourceId, Long operator) {
    this.roleId = roleId;
    this.resourceId = resourceId;
    this.createdBy = operator;
    this.createdAt = HighDate.mockInstant();
  }
}

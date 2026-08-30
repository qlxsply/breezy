package com.corwin.system.resource.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;

/**
 * Entity representing the binding between a resource (button) and a permission code.
 *
 * <p>In the resource model, the resource tree is represented by {@link Resource} and permission
 * codes by {@link Permission}. This entity establishes the many-to-many relationship between them.
 *
 * <p>Key notes:
 *
 * <ul>
 *   <li>A {@code resourceId} must point to a {@link ResourceType#BUTTON} resource.
 *   <li>A button resource can be bound to one or more permission codes.
 *   <li>A permission code can be reused across multiple button resources.
 *   <li>This entity only persists the binding; type validation should be done by domain services.
 * </ul>
 *
 * @author Corwin
 */
@Getter
@Entity
@Table(
    name = "sys_resource_permission",
    indexes = {
      @Index(
          name = "idx_sys_resource_permission_unique",
          columnList = "resource_id,permission_id",
          unique = true),
      @Index(name = "idx_sys_resource_permission_resource_id", columnList = "resource_id"),
      @Index(name = "idx_sys_resource_permission_permission_id", columnList = "permission_id")
    })
public class ResourcePermission {

  /** Primary key ID. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Resource ID referencing {@link Resource#getId()}.
   *
   * <p>Business constraints: the referenced resource must exist, be enabled, and be of type {@link
   * ResourceType#BUTTON}. No {@code @ManyToOne} is used to avoid unnecessary cascade queries when
   * loading the resource tree.
   */
  @Column(name = "resource_id", nullable = false)
  private Long resourceId;

  /**
   * Permission code ID referencing {@link Permission#getId()}.
   *
   * <p>Business constraints: the referenced permission must exist and be enabled.
   */
  @Column(name = "permission_id", nullable = false)
  private Long permissionId;

  /**
   * Whether this binding is system-defined. System bindings are typically created by initialization
   * scripts and should not be deleted by regular users.
   */
  @Column(name = "system_builtin", nullable = false)
  private Boolean systemBuiltin;

  /** Creation timestamp. */
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** JPA 构造函数。 */
  protected ResourcePermission() {}

  /**
   * Creates a resource-permission binding.
   *
   * @param resourceId resource ID (must reference a BUTTON-type resource)
   * @param permissionId permission code ID
   * @param systemBuiltin whether this binding is system-defined
   */
  public ResourcePermission(Long resourceId, Long permissionId, Boolean systemBuiltin) {
    this.resourceId = requireNonNull(resourceId, "资源 ID 不能为空");
    this.permissionId = requireNonNull(permissionId, "权限码 ID 不能为空");
    this.systemBuiltin = defaultBoolean(systemBuiltin, false);
    this.createdAt = HighDate.mockInstant();
  }

  /**
   * Checks whether this binding belongs to the specified resource.
   *
   * @param resourceId the resource ID to test against
   * @return true if this binding belongs to the given resource
   */
  public boolean belongsToResource(Long resourceId) {
    return this.resourceId != null && this.resourceId.equals(resourceId);
  }

  /**
   * Checks whether this binding points to the specified permission code.
   *
   * @param permissionId the permission ID to test against
   * @return true if this binding points to the given permission
   */
  public boolean pointsToPermission(Long permissionId) {
    return this.permissionId != null && this.permissionId.equals(permissionId);
  }

  /**
   * Checks whether this binding is system-defined.
   *
   * @return true if this is a system-defined binding
   */
  public boolean isSystemBuiltin() {
    return Boolean.TRUE.equals(this.systemBuiltin);
  }

  private static <T> T requireNonNull(T value, String message) {
    if (value == null) {
      throw new IllegalArgumentException(message);
    }
    return value;
  }

  private static Boolean defaultBoolean(Boolean value, Boolean defaultValue) {
    return value == null ? defaultValue : value;
  }
}

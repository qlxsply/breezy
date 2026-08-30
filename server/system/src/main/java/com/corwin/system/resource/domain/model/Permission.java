package com.corwin.system.resource.domain.model;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;

/**
 * Permission code entity.
 *
 * <p>Represents a granular permission code that can be assigned to roles and bound to resources.
 * Each permission has a unique code, a display name, and a user scope that determines which user
 * types can be granted this permission.
 *
 * @author Corwin 2026/4/19
 */
@Getter
@Entity
@Table(
    name = "sys_permission",
    indexes = {
      @Index(name = "idx_sys_permission_code", columnList = "code", unique = true),
      @Index(name = "idx_sys_permission_user_scope", columnList = "user_scope")
    })
public class Permission {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Unique permission code (e.g. "system:user:create"). */
  @Column(name = "code", nullable = false, length = 128)
  private String code;

  /** Display name for the permission. */
  @Column(name = "name", nullable = false, length = 128)
  private String name;

  /** User type scope that this permission applies to (e.g. ADMIN, USER). */
  @Enumerated(EnumType.STRING)
  @Column(name = "user_scope", nullable = false, length = 16)
  private UserType userScope;

  /** Creation timestamp. */
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** Last update timestamp. */
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Permission() {}

  /**
   * Creates a new permission.
   *
   * @param code the unique permission code
   * @param name the display name
   * @param userScope the user type scope this permission applies to
   */
  public Permission(String code, String name, UserType userScope) {
    Instant now = HighDate.mockInstant();
    this.code = code;
    this.name = name;
    this.userScope = userScope;
    this.createdAt = now;
    this.updatedAt = now;
  }

  /**
   * Updates the permission name and user scope.
   *
   * @param name the new display name
   * @param userScope the new user type scope
   */
  public void update(String name, UserType userScope) {
    this.name = name;
    this.userScope = userScope;
    touch();
  }

  private void touch() {
    this.updatedAt = HighDate.mockInstant();
  }
}

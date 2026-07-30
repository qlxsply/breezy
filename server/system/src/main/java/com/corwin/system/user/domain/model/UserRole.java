package com.corwin.system.user.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * Represents the many-to-many association between a user and a role,
 * mapped to the {@code sys_user_role} table.
 *
 * @author Corwin 2026/1/23
 */
@Getter
@Entity
@Table(name = "sys_user_role",
        indexes = {@Index(name = "idx_sys_user_role_unique", columnList = "user_id,role_id", unique = true),
                @Index(name = "idx_sys_user_role_user_id", columnList = "user_id"),
                @Index(name = "idx_sys_user_role_role_id", columnList = "role_id")})
public class UserRole {
    /** Primary key, auto-incremented. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user ID in this association. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** The role ID in this association. */
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    /** The operator who created this role assignment. */
    @Column(name = "created_by")
    private Long createdBy;

    /** Timestamp when the role assignment was created. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserRole() {
    }

    public UserRole(Long userId, Long roleId, Long operator) {
        this.userId = userId;
        this.roleId = roleId;
        this.createdBy = operator;
        this.createdAt = HighDate.mockInstant();
    }
}

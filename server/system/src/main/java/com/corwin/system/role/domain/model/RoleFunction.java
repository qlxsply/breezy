package com.corwin.system.role.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * 角色功能关系
 *
 * @author Corwin 2026/4/19
 */
@Getter
@Entity
@Table(name = "sys_role_function",
        indexes = {@Index(name = "idx_sys_role_function_unique", columnList = "role_id,function_id", unique = true),
                @Index(name = "idx_sys_role_function_role_id", columnList = "role_id"),
                @Index(name = "idx_sys_role_function_function_id", columnList = "function_id")})
public class RoleFunction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "function_id", nullable = false)
    private Long functionId;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RoleFunction() {
    }

    public RoleFunction(Long roleId, Long functionId, Long operator) {
        this.roleId = roleId;
        this.functionId = functionId;
        this.createdBy = operator;
        this.createdAt = HighDate.mockInstant();
    }
}

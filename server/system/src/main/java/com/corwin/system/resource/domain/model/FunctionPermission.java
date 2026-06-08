package com.corwin.system.resource.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * 功能权限码关系
 *
 * @author Corwin 2026/4/19
 */
@Getter
@Entity
@Table(name = "sys_function_permission", indexes = {
        @Index(name = "idx_sys_function_permission_unique", columnList = "function_id,permission_id", unique = true),
        @Index(name = "idx_sys_function_permission_function_id", columnList = "function_id"),
        @Index(name = "idx_sys_function_permission_permission_id", columnList = "permission_id")})
public class FunctionPermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "function_id", nullable = false)
    private Long functionId;

    @Column(name = "permission_id", nullable = false)
    private Long permissionId;

    @Column(name = "system_builtin", nullable = false)
    private Boolean systemBuiltin;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FunctionPermission() {
    }

    public FunctionPermission(Long functionId, Long permissionId, Boolean systemBuiltin) {
        this.functionId = functionId;
        this.permissionId = permissionId;
        this.systemBuiltin = systemBuiltin;
        this.createdAt = HighDate.mockInstant();
    }
}

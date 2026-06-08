package com.corwin.system.resource.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * 权限码
 *
 * @author Corwin 2026/4/19
 */
@Getter
@Entity
@Table(name = "sys_permission",
        indexes = {@Index(name = "idx_sys_permission_code", columnList = "code", unique = true),
                @Index(name = "idx_sys_permission_user_scope", columnList = "user_scope")})
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 128)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_scope", nullable = false, length = 16)
    private PermissionUserScope userScope;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "system_builtin", nullable = false)
    private Boolean systemBuiltin;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Permission() {
    }

    public Permission(String code, String name, PermissionUserScope userScope, String description,
            Boolean systemBuiltin) {
        Instant now = HighDate.mockInstant();
        this.code = code;
        this.name = name;
        this.userScope = userScope;
        this.description = description;
        this.systemBuiltin = systemBuiltin;
        this.enabled = true;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String name, PermissionUserScope userScope, String description, Boolean systemBuiltin,
            Boolean enabled) {
        this.name = name;
        this.userScope = userScope;
        this.description = description;
        this.systemBuiltin = systemBuiltin;
        this.enabled = enabled;
        touch();
    }

    public void enable() {
        this.enabled = true;
        touch();
    }

    public void disable() {
        this.enabled = false;
        touch();
    }

    private void touch() {
        this.updatedAt = HighDate.mockInstant();
    }
}

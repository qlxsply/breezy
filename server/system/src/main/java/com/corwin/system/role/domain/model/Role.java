package com.corwin.system.role.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * @author Corwin 2026/1/23
 */
@Getter
@Entity
@Table(name = "sys_role", indexes = {@Index(name = "idx_sys_role_code", columnList = "code", unique = true),
        @Index(name = "idx_sys_role_enabled", columnList = "enabled")})
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 128)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "system_builtin", nullable = false)
    private Boolean systemBuiltin;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Role() {
    }

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

    public void enable(Long operator) {
        this.enabled = true;
        touch(operator);
    }

    public void disable(Long operator) {
        this.enabled = false;
        touch(operator);
    }

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

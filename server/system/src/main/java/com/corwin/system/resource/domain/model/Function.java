package com.corwin.system.resource.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * 功能
 *
 * @author Corwin 2026/4/19
 */
@Getter
@Entity
@Table(name = "sys_function",
        indexes = {@Index(name = "idx_sys_function_code", columnList = "code", unique = true),
                @Index(name = "idx_sys_function_type", columnList = "function_type")})
public class Function {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 128)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "function_type", nullable = false, length = 16)
    private FunctionType functionType;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "system_builtin", nullable = false)
    private Boolean systemBuiltin;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Function() {
    }

    public Function(String code, String name, FunctionType functionType, String description, Boolean systemBuiltin) {
        Instant now = HighDate.mockInstant();
        this.code = code;
        this.name = name;
        this.functionType = functionType;
        this.description = description;
        this.enabled = true;
        this.systemBuiltin = systemBuiltin;
        this.createdAt = now;
        this.updatedAt = now;
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

package com.corwin.system.resource.domain.model;

import com.corwin.framework.constant.UserType;
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
    private UserType userScope;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Permission() {
    }

    public Permission(String code, String name, UserType userScope) {
        Instant now = HighDate.mockInstant();
        this.code = code;
        this.name = name;
        this.userScope = userScope;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String name, UserType userScope) {
        this.name = name;
        this.userScope = userScope;
        touch();
    }

    private void touch() {
        this.updatedAt = HighDate.mockInstant();
    }
}

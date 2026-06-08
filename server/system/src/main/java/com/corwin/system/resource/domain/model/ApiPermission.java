package com.corwin.system.resource.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 *
 * @author Corwin 2026/4/19
 */
@Getter
@Entity
@Table(name = "sys_api_permission",
        indexes = {@Index(name = "idx_sys_api_permission_unique", columnList = "api_id,permission_id", unique = true),
                @Index(name = "idx_sys_api_permission_api_id", columnList = "api_id"),
                @Index(name = "idx_sys_api_permission_permission_id", columnList = "permission_id")})
public class ApiPermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_id", nullable = false)
    private Long apiId;

    @Column(name = "permission_id", nullable = false)
    private Long permissionId;

    @Column(name = "system_builtin", nullable = false)
    private Boolean systemBuiltin;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ApiPermission() {
    }

    public ApiPermission(Long apiId, Long permissionId, Boolean systemBuiltin) {
        this.apiId = apiId;
        this.permissionId = permissionId;
        this.systemBuiltin = systemBuiltin;
        this.createdAt = HighDate.mockInstant();
    }
}

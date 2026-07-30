package com.corwin.system.resource.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * API-permission binding entity.
 *
 * <p>Links an API endpoint to a permission code, defining which permission
 * is required to access the API. A single API can be bound to multiple permissions,
 * and a single permission can be referenced by multiple APIs.</p>
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

    /**
     * API ID referencing the bound API endpoint.
     */
    @Column(name = "api_id", nullable = false)
    private Long apiId;

    /**
     * Permission ID referencing the bound permission code.
     */
    @Column(name = "permission_id", nullable = false)
    private Long permissionId;

    /**
     * Whether this API-permission binding is system-defined.
     */
    @Column(name = "system_builtin", nullable = false)
    private Boolean systemBuiltin;

    /**
     * Creation timestamp.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ApiPermission() {
    }

    /**
     * Creates an API-permission binding.
     *
     * @param apiId         the API ID
     * @param permissionId  the permission ID
     * @param systemBuiltin whether this binding is system-defined
     */
    public ApiPermission(Long apiId, Long permissionId, Boolean systemBuiltin) {
        this.apiId = apiId;
        this.permissionId = permissionId;
        this.systemBuiltin = systemBuiltin;
        this.createdAt = HighDate.mockInstant();
    }
}

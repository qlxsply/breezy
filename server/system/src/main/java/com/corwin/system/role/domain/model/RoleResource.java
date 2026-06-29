package com.corwin.system.role.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;

/**
 * @author Corwin 2026/6/29
 */
@Getter
@Entity
@Table(name = "sys_role_resource",
        indexes = {@Index(name = "idx_sys_role_resource_unique", columnList = "role_id,resource_id", unique = true),
                @Index(name = "idx_sys_role_resource_role_id", columnList = "role_id"),
                @Index(name = "idx_sys_role_resource_resource_id", columnList = "resource_id")})
public class RoleResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RoleResource() {
    }

    public RoleResource(Long roleId, Long resourceId, Long operator) {
        this.roleId = roleId;
        this.resourceId = resourceId;
        this.createdBy = operator;
        this.createdAt = HighDate.mockInstant();
    }
}

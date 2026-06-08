package com.corwin.system.normalfeature.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * 用户功能权限码关系
 *
 * @author Corwin 2026/4/19
 */
@Getter
@Entity
@Table(name = "sys_normal_feature_permission", indexes = {
        @Index(name = "idx_sys_normal_feature_permission_unique", columnList = "feature_id,permission_id",
                unique = true),
        @Index(name = "idx_sys_normal_feature_permission_feature_id", columnList = "feature_id"),
        @Index(name = "idx_sys_normal_feature_permission_permission_id", columnList = "permission_id")})
public class NormalFeaturePermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "feature_id", nullable = false)
    private Long featureId;

    @Column(name = "permission_id", nullable = false)
    private Long permissionId;

    @Column(name = "system_builtin", nullable = false)
    private Boolean systemBuiltin;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected NormalFeaturePermission() {
    }

    public NormalFeaturePermission(Long featureId, Long permissionId, Boolean systemBuiltin) {
        this.featureId = featureId;
        this.permissionId = permissionId;
        this.systemBuiltin = systemBuiltin;
        this.createdAt = HighDate.mockInstant();
    }
}

package com.corwin.system.userfeature.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * 产品功能权限码绑定。
 *
 * <p>表示应用内功能与后端权限码之间的绑定关系，用于后端 API 权限校验。</p>
 *
 * @author Corwin
 */
@Getter
@Entity
@Table(name = "sys_product_feature_permission_binding", indexes = {
        @Index(name = "idx_sys_product_feature_permission_binding_unique", columnList = "feature_id,permission_id",
                unique = true),
        @Index(name = "idx_sys_product_feature_permission_binding_application_id", columnList = "application_id"),
        @Index(name = "idx_sys_product_feature_permission_binding_feature_id", columnList = "feature_id"),
        @Index(name = "idx_sys_product_feature_permission_binding_permission_id", columnList = "permission_id")})
public class ProductFeaturePermissionBinding {

    /**
     * 主键 ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 产品应用 ID。
     *
     * <p>逻辑引用 {@link ProductApplication#getId()}，用于查询和一致性校验。</p>
     */
    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    /**
     * 产品功能 ID。
     *
     * <p>逻辑引用 {@link ProductFeature#getId()}，数据库层面不创建外键。</p>
     */
    @Column(name = "feature_id", nullable = false)
    private Long featureId;

    /**
     * 权限码 ID。
     *
     * <p>逻辑引用权限码实体主键，数据库层面不创建外键。</p>
     */
    @Column(name = "permission_id", nullable = false)
    private Long permissionId;

    /**
     * 是否系统内置。
     */
    @Column(name = "system_built_in", nullable = false)
    private Boolean systemBuiltIn;

    /**
     * 创建时间。
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * JPA 构造方法。
     */
    protected ProductFeaturePermissionBinding() {
    }

    /**
     * 创建产品功能权限码绑定。
     *
     * @param applicationId 产品应用 ID
     * @param featureId     产品功能 ID
     * @param permissionId  权限码 ID
     * @param systemBuiltIn 是否系统内置
     */
    public ProductFeaturePermissionBinding(Long applicationId, Long featureId, Long permissionId,
            Boolean systemBuiltIn) {
        this.applicationId = applicationId;
        this.featureId = featureId;
        this.permissionId = permissionId;
        this.systemBuiltIn = systemBuiltIn;
        this.createdAt = HighDate.mockInstant();
    }
}

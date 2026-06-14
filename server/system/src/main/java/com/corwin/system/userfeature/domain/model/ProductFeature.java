package com.corwin.system.userfeature.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * 产品功能。
 *
 * <p>表示产品应用内的细粒度能力，例如页面入口、按钮、子功能或业务操作。功能可以绑定后端权限码。</p>
 *
 * @author Corwin
 */
@Getter
@Entity
@Table(name = "sys_product_feature", indexes = {
        @Index(name = "idx_sys_product_feature_unique", columnList = "application_id,feature_code", unique = true),
        @Index(name = "idx_sys_product_feature_application_id", columnList = "application_id"),
        @Index(name = "idx_sys_product_feature_type", columnList = "feature_type"),
        @Index(name = "idx_sys_product_feature_enabled", columnList = "enabled")})
public class ProductFeature {

    /**
     * 主键 ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所属应用 ID。
     *
     * <p>逻辑引用 {@link ProductApplication#getId()}，数据库层面不创建外键。</p>
     */
    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    /**
     * 功能编码。
     *
     * <p>在同一应用下唯一，例如 VIEW、EXPORT、BATCH_ANALYZE。</p>
     */
    @Column(name = "feature_code", nullable = false, length = 128)
    private String featureCode;

    /**
     * 功能名称。
     */
    @Column(name = "feature_name", nullable = false, length = 128)
    private String featureName;

    /**
     * 功能类型。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "feature_type", nullable = false, length = 32)
    private ProductFeatureType featureType;

    /**
     * 功能描述。
     */
    @Column(name = "description", length = 512)
    private String description;

    /**
     * 是否全局启用。
     *
     * <p>功能关闭后，用户包授权和用户功能特例均不生效。</p>
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    /**
     * 是否系统内置。
     */
    @Column(name = "system_built_in", nullable = false)
    private Boolean systemBuiltIn;

    /**
     * 展示排序值。
     */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    /**
     * 创建人用户 ID。
     */
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * 创建时间。
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * 最后更新人用户 ID。
     */
    @Column(name = "updated_by")
    private Long updatedBy;

    /**
     * 最后更新时间。
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * JPA 构造方法。
     */
    protected ProductFeature() {
    }

    /**
     * 创建产品功能。
     *
     * @param applicationId 所属应用 ID
     * @param featureCode   功能编码
     * @param featureName   功能名称
     * @param featureType   功能类型
     * @param description   功能描述
     * @param enabled       是否全局启用
     * @param systemBuiltIn 是否系统内置
     * @param displayOrder  展示排序值
     * @param operator      操作人用户 ID
     */
    public ProductFeature(Long applicationId, String featureCode, String featureName, ProductFeatureType featureType,
            String description, Boolean enabled, Boolean systemBuiltIn, Integer displayOrder, Long operator) {
        Instant now = HighDate.mockInstant();
        this.applicationId = applicationId;
        this.featureCode = featureCode;
        this.featureName = featureName;
        this.featureType = featureType;
        this.description = description;
        this.enabled = enabled;
        this.systemBuiltIn = systemBuiltIn;
        this.displayOrder = displayOrder;
        this.createdBy = operator;
        this.createdAt = now;
        this.updatedBy = operator;
        this.updatedAt = now;
    }

    /**
     * 更新功能基础信息。
     *
     * @param featureCode  功能编码
     * @param featureName  功能名称
     * @param featureType  功能类型
     * @param description  功能描述
     * @param displayOrder 展示排序值
     * @param operator     操作人用户 ID
     */
    public void update(String featureCode, String featureName, ProductFeatureType featureType, String description,
            Integer displayOrder, Long operator) {
        this.featureCode = featureCode;
        this.featureName = featureName;
        this.featureType = featureType;
        this.description = description;
        this.displayOrder = displayOrder;
        touch(operator);
    }

    /**
     * 启用功能。
     *
     * @param operator 操作人用户 ID
     */
    public void enable(Long operator) {
        this.enabled = true;
        touch(operator);
    }

    /**
     * 关闭功能。
     *
     * @param operator 操作人用户 ID
     */
    public void disable(Long operator) {
        this.enabled = false;
        touch(operator);
    }

    /**
     * 刷新审计字段。
     *
     * @param operator 操作人用户 ID
     */
    private void touch(Long operator) {
        this.updatedBy = operator;
        this.updatedAt = HighDate.mockInstant();
    }
}

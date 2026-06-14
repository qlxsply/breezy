package com.corwin.system.userfeature.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * 产品应用。
 *
 * <p>表示系统中可对用户开放的完整应用入口，负责前端入口控制和应用级全局开关，不直接绑定后端权限码。</p>
 *
 * @author Corwin 2026/6/14
 */
@Getter
@Entity
@Table(name = "sys_product_application",
        indexes = {@Index(name = "idx_sys_product_application_code", columnList = "application_code", unique = true),
                @Index(name = "idx_sys_product_application_enabled", columnList = "enabled")})
public class ProductApplication {

    /**
     * 主键 ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 应用编码。
     *
     * <p>用于业务代码稳定识别应用，例如 DATA_ANALYSIS、AI_WRITING。</p>
     */
    @Column(name = "application_code", nullable = false, length = 128)
    private String applicationCode;

    /**
     * 应用名称。
     *
     * <p>用于管理后台、用户权益页、前端菜单等展示场景。</p>
     */
    @Column(name = "application_name", nullable = false, length = 128)
    private String applicationName;

    /**
     * 应用描述。
     */
    @Column(name = "description", length = 512)
    private String description;

    /**
     * 应用图标。
     *
     * <p>用于前端入口展示，可直接存储图标编码、静态资源 code 或其他前端约定值。</p>
     */
    @Column(name = "icon", length = 256)
    private String icon;

    /**
     * 应用前端路径。
     *
     * <p>用于前端路由跳转，例如 /jsonfmt、/storage。</p>
     */
    @Column(name = "route_path", length = 512)
    private String routePath;

    /**
     * 应用前端组件标识。
     *
     * <p>用于前端按约定加载页面组件，例如 pages/JsonFormatterPage.vue。</p>
     */
    @Column(name = "component_path", length = 512)
    private String componentPath;

    /**
     * 是否全局启用。
     *
     * <p>应用关闭后，用户包授权、用户特例和应用内功能授权均不生效。</p>
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    /**
     * 是否系统内置。
     *
     * <p>系统内置应用通常由程序初始化，不建议后台删除。</p>
     */
    @Column(name = "system_built_in", nullable = false)
    private Boolean systemBuiltIn;

    /**
     * 展示排序值。
     *
     * <p>数值越小，排序越靠前。</p>
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
    protected ProductApplication() {
    }

    /**
     * 创建产品应用。
     *
     * @param applicationCode 应用编码
     * @param applicationName 应用名称
     * @param description     应用描述
     * @param icon            应用图标
     * @param routePath       应用前端路径
     * @param componentPath   应用前端组件标识
     * @param enabled         是否全局启用
     * @param systemBuiltIn   是否系统内置
     * @param displayOrder    展示排序值
     * @param operator        操作人用户 ID
     */
    public ProductApplication(String applicationCode, String applicationName, String description, String icon,
            String routePath, String componentPath, Boolean enabled, Boolean systemBuiltIn, Integer displayOrder,
            Long operator) {
        Instant now = HighDate.mockInstant();
        this.applicationCode = applicationCode;
        this.applicationName = applicationName;
        this.description = description;
        this.icon = icon;
        this.routePath = routePath;
        this.componentPath = componentPath;
        this.enabled = enabled;
        this.systemBuiltIn = systemBuiltIn;
        this.displayOrder = displayOrder;
        this.createdBy = operator;
        this.createdAt = now;
        this.updatedBy = operator;
        this.updatedAt = now;
    }

    /**
     * 更新应用基础信息。
     *
     * @param applicationCode 应用编码
     * @param applicationName 应用名称
     * @param description     应用描述
     * @param icon            应用图标
     * @param routePath       应用前端路径
     * @param componentPath   应用前端组件标识
     * @param displayOrder    展示排序值
     * @param operator        操作人用户 ID
     */
    public void update(String applicationCode, String applicationName, String description, String icon,
            String routePath, String componentPath, Integer displayOrder, Long operator) {
        this.applicationCode = applicationCode;
        this.applicationName = applicationName;
        this.description = description;
        this.icon = icon;
        this.routePath = routePath;
        this.componentPath = componentPath;
        this.displayOrder = displayOrder;
        touch(operator);
    }

    /**
     * 启用应用。
     *
     * @param operator 操作人用户 ID
     */
    public void enable(Long operator) {
        this.enabled = true;
        touch(operator);
    }

    /**
     * 关闭应用。
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

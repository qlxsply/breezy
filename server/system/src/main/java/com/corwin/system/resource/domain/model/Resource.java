package com.corwin.system.resource.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * 系统资源。
 *
 * <p>
 * 该实体用于统一表示管理后台中的目录、菜单、功能、按钮资源。
 * </p>
 *
 * <p>
 * 原先可以拆分为 Menu、Function、MenuFunction 等多个概念。
 * 合并后，通过 resourceType 字段区分资源节点类型，通过 parentId 构建完整资源树。
 * </p>
 *
 * <p>
 * 支持的资源树结构：
 * </p>
 *
 * <pre>
 * resources
 * ├── DIRECTORY
 * │   ├── DIRECTORY
 * │   └── MENU
 * │       ├── MENU
 * │       ├── FUNCTION
 * │       │   └── BUTTON
 * │       └── BUTTON
 * └── MENU
 *     ├── MENU
 *     ├── FUNCTION
 *     │   └── BUTTON
 *     └── BUTTON
 * </pre>
 *
 * <p>
 * 资源类型约束：
 * </p>
 *
 * <ul>
 *     <li>DIRECTORY 下只能挂载 DIRECTORY、MENU。</li>
 *     <li>MENU 下只能挂载 MENU、FUNCTION、BUTTON。</li>
 *     <li>FUNCTION 下只能挂载 BUTTON。</li>
 *     <li>BUTTON 是叶子资源，不允许挂载任何子资源。</li>
 * </ul>
 *
 * <p>
 * 权限码绑定约束：
 * </p>
 *
 * <ul>
 *     <li>权限码不是资源树节点。</li>
 *     <li>只有 BUTTON 类型资源允许绑定权限码。</li>
 *     <li>按钮和权限码通过 {@link ResourcePermission} 关联。</li>
 * </ul>
 *
 * <p>
 * 字段说明：
 * </p>
 *
 * <ul>
 *     <li>code：资源编码，全局唯一，建议使用业务语义编码。</li>
 *     <li>parentId：父资源 ID，根资源为空。</li>
 *     <li>resourceType：资源类型。</li>
 *     <li>path：前端路由路径，通常用于 MENU 或 FUNCTION。</li>
 *     <li>component：前端组件路径，通常用于 MENU 或 FUNCTION。</li>
 *     <li>icon：图标，通常用于 DIRECTORY 或 MENU。</li>
 *     <li>sortNo：同级排序号。</li>
 *     <li>visible：是否可见，主要影响前端展示。</li>
 *     <li>enabled：是否启用，主要影响资源是否生效。</li>
 *     <li>defaultEntry：是否默认入口，通常用于菜单或功能。</li>
 *     <li>systemBuiltin：是否系统内置资源。</li>
 * </ul>
 *
 * @author Corwin
 */
@Getter
@Entity
@Table(name = "sys_resource", indexes = {@Index(name = "idx_sys_resource_code", columnList = "code", unique = true),
        @Index(name = "idx_sys_resource_parent_id", columnList = "parent_id"),
        @Index(name = "idx_sys_resource_type", columnList = "resource_type"),
        @Index(name = "idx_sys_resource_sort_no", columnList = "sort_no")})
public class Resource {

    /**
     * 资源 ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 资源编码。
     *
     * <p>
     * 全局唯一。
     * 建议使用稳定的业务语义编码，例如：
     * </p>
     *
     * <pre>
     * system
     * system.user
     * system.user.create
     * system.user.delete
     * </pre>
     */
    @Column(name = "code", nullable = false, length = 128)
    private String code;

    /**
     * 父资源 ID。
     *
     * <p>
     * 顶级资源的 parentId 为空。
     * 通过 parentId 构建完整资源树。
     * </p>
     */
    @Column(name = "parent_id")
    private Long parentId;

    /**
     * 资源名称。
     *
     * <p>
     * 用于后台管理界面展示。
     * </p>
     */
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    /**
     * 资源类型。
     *
     * <p>
     * 用于区分目录、菜单、功能、按钮。
     * </p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 32)
    private ResourceType resourceType;

    /**
     * 前端路由路径。
     *
     * <p>
     * 通常用于 MENU 或 FUNCTION。
     * DIRECTORY 和 BUTTON 一般不需要配置。
     * </p>
     */
    @Column(name = "path", length = 512)
    private String path;

    /**
     * 前端组件路径。
     *
     * <p>
     * 通常用于 MENU 或 FUNCTION。
     * DIRECTORY 和 BUTTON 一般不需要配置。
     * </p>
     */
    @Column(name = "component", length = 512)
    private String component;

    /**
     * 图标。
     *
     * <p>
     * 通常用于 DIRECTORY 或 MENU。
     * FUNCTION 和 BUTTON 一般不需要配置。
     * </p>
     */
    @Column(name = "icon", length = 128)
    private String icon;

    /**
     * 排序号。
     *
     * <p>
     * 用于同级资源排序。
     * 数值越小越靠前。
     * </p>
     */
    @Column(name = "sort_no", nullable = false)
    private Integer sortNo;

    /**
     * 是否可见。
     *
     * <p>
     * 主要影响前端是否展示。
     * 例如隐藏菜单、隐藏功能可以 visible=false。
     * </p>
     */
    @Column(name = "visible", nullable = false)
    private Boolean visible;

    /**
     * 是否启用。
     *
     * <p>
     * enabled=false 表示资源停用。
     * 停用资源一般不参与权限分配、菜单渲染或按钮鉴权。
     * </p>
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    /**
     * 是否默认入口。
     *
     * <p>
     * 通常用于 MENU 或 FUNCTION。
     * 例如某个菜单下存在多个功能页时，可以指定其中一个功能为默认入口。
     * </p>
     */
    @Column(name = "default_entry", nullable = false)
    private Boolean defaultEntry;

    /**
     * 是否系统内置资源。
     *
     * <p>
     * 系统内置资源通常不允许被普通用户删除。
     * </p>
     */
    @Column(name = "system_builtin", nullable = false)
    private Boolean systemBuiltin;

    /**
     * 备注。
     */
    @Column(name = "remark", length = 512)
    private String remark;

    /**
     * 创建时间。
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * 更新时间。
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * JPA 构造函数。
     */
    protected Resource() {
    }

    /**
     * 创建资源。
     *
     * @param code          资源编码
     * @param parentId      父资源 ID，顶级资源为空
     * @param name          资源名称
     * @param resourceType  资源类型
     * @param path          前端路由路径
     * @param component     前端组件路径
     * @param icon          图标
     * @param sortNo        排序号
     * @param visible       是否可见
     * @param defaultEntry  是否默认入口
     * @param systemBuiltin 是否系统内置资源
     * @param remark        备注
     */
    public Resource(String code, Long parentId, String name, ResourceType resourceType, String path, String component,
            String icon, Integer sortNo, Boolean visible, Boolean defaultEntry, Boolean systemBuiltin, String remark) {
        Instant now = HighDate.mockInstant();

        this.code = requireNotBlank(code, "资源编码不能为空");
        this.parentId = parentId;
        this.name = requireNotBlank(name, "资源名称不能为空");
        this.resourceType = requireNonNull(resourceType, "资源类型不能为空");

        this.path = normalizeBlank(path);
        this.component = normalizeBlank(component);
        this.icon = normalizeBlank(icon);

        this.sortNo = requireNonNull(sortNo, "排序号不能为空");
        this.visible = defaultBoolean(visible, true);
        this.enabled = true;
        this.defaultEntry = defaultBoolean(defaultEntry, false);
        this.systemBuiltin = defaultBoolean(systemBuiltin, false);
        this.remark = normalizeBlank(remark);

        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 修改资源基础信息。
     *
     * @param name   资源名称
     * @param icon   图标
     * @param remark 备注
     */
    public void modifyBasicInfo(String name, String icon, String remark) {
        this.name = requireNotBlank(name, "资源名称不能为空");
        this.icon = normalizeBlank(icon);
        this.remark = normalizeBlank(remark);
        touch();
    }

    /**
     * 修改前端路由信息。
     *
     * <p>
     * 通常只建议 MENU 或 FUNCTION 类型资源配置 path/component。
     * </p>
     *
     * @param path      前端路由路径
     * @param component 前端组件路径
     */
    public void modifyRoute(String path, String component) {
        this.path = normalizeBlank(path);
        this.component = normalizeBlank(component);
        touch();
    }

    /**
     * 修改父资源。
     *
     * <p>
     * 该方法只修改 parentId。
     * 父子类型是否合法应由领域服务根据 {@link ResourceType#canHaveChild(ResourceType)} 统一校验。
     * </p>
     *
     * @param parentId 父资源 ID
     */
    public void moveTo(Long parentId) {
        this.parentId = parentId;
        touch();
    }

    /**
     * 修改排序号。
     *
     * @param sortNo 排序号
     */
    public void changeSortNo(Integer sortNo) {
        this.sortNo = requireNonNull(sortNo, "排序号不能为空");
        touch();
    }

    /**
     * 启用资源。
     */
    public void enable() {
        this.enabled = true;
        touch();
    }

    /**
     * 停用资源。
     */
    public void disable() {
        this.enabled = false;
        touch();
    }

    /**
     * 显示资源。
     */
    public void show() {
        this.visible = true;
        touch();
    }

    /**
     * 隐藏资源。
     */
    public void hide() {
        this.visible = false;
        touch();
    }

    /**
     * 标记为默认入口。
     */
    public void markDefaultEntry() {
        this.defaultEntry = true;
        touch();
    }

    /**
     * 取消默认入口。
     */
    public void cancelDefaultEntry() {
        this.defaultEntry = false;
        touch();
    }

    /**
     * 判断是否为目录资源。
     *
     * @return true 表示当前资源是目录
     */
    public boolean isDirectory() {
        return this.resourceType == ResourceType.DIRECTORY;
    }

    /**
     * 判断是否为菜单资源。
     *
     * @return true 表示当前资源是菜单
     */
    public boolean isMenu() {
        return this.resourceType == ResourceType.MENU;
    }

    /**
     * 判断是否为功能资源。
     *
     * @return true 表示当前资源是功能
     */
    public boolean isFunction() {
        return this.resourceType == ResourceType.FUNCTION;
    }

    /**
     * 判断是否为按钮资源。
     *
     * @return true 表示当前资源是按钮
     */
    public boolean isButton() {
        return this.resourceType == ResourceType.BUTTON;
    }

    /**
     * 判断当前资源是否允许挂载子资源。
     *
     * @return true 表示允许挂载子资源
     */
    public boolean canHaveChildren() {
        return this.resourceType.canHaveChildren();
    }

    /**
     * 判断当前资源是否允许挂载指定子资源。
     *
     * @param childType 子资源类型
     * @return true 表示允许挂载
     */
    public boolean canHaveChild(ResourceType childType) {
        return this.resourceType.canHaveChild(childType);
    }

    /**
     * 判断当前资源是否允许绑定权限码。
     *
     * @return true 表示允许绑定权限码
     */
    public boolean canBindPermission() {
        return this.resourceType.canBindPermission();
    }

    /**
     * 更新时间。
     */
    private void touch() {
        this.updatedAt = HighDate.mockInstant();
    }

    private static String requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static <T> T requireNonNull(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static Boolean defaultBoolean(Boolean value, Boolean defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}

package com.corwin.system.resource.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * System resource entity representing a node in the admin resource tree.
 *
 * <p>This unified entity represents directories, menus, functions, and buttons,
 * distinguished by the {@code resourceType} field. The tree hierarchy is built
 * using the {@code parentId} field.</p>
 *
 * <p>Supported tree structure:</p>
 * <pre>
 * DIRECTORY
 * ├── DIRECTORY
 * └── MENU
 *     ├── MENU
 *     ├── FUNCTION
 *     │   └── BUTTON
 *     └── BUTTON
 * </pre>
 *
 * <p>Key constraints:</p>
 * <ul>
 *   <li>Permission codes are not resource tree nodes.</li>
 *   <li>Only BUTTON-type resources can bind permission codes.</li>
 *   <li>Permission bindings are managed via {@link ResourcePermission}.</li>
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
     * Primary key ID.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique resource code with business semantics (e.g. "system", "system.user", "system.user.create").
     */
    @Column(name = "code", nullable = false, length = 128)
    private String code;

    /**
     * Parent resource ID for building the tree hierarchy. Null for root resources.
     */
    @Column(name = "parent_id")
    private Long parentId;

    /**
     * Resource display name shown in the admin interface.
     */
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    /**
     * Resource type distinguishing directory, menu, function, or button.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 32)
    private ResourceType resourceType;

    /**
     * Front-end route path, typically used for MENU or FUNCTION resources.
     */
    @Column(name = "path", length = 512)
    private String path;

    /**
     * Front-end component path, typically used for MENU or FUNCTION resources.
     */
    @Column(name = "component", length = 512)
    private String component;

    /**
     * Icon identifier, typically used for DIRECTORY or MENU resources.
     */
    @Column(name = "icon", length = 128)
    private String icon;

    /**
     * Sort order among siblings. Lower values appear first.
     */
    @Column(name = "sort_no", nullable = false)
    private Integer sortNo;

    /**
     * Whether the resource is visible in the front-end UI.
     */
    @Column(name = "visible", nullable = false)
    private Boolean visible;

    /**
     * Whether the resource is enabled. Disabled resources do not participate in authorization.
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    /**
     * Whether this resource is the default entry point (typically for MENU or FUNCTION).
     */
    @Column(name = "default_entry", nullable = false)
    private Boolean defaultEntry;

    /**
     * Whether this resource is system-built. System-built resources should not be deleted by regular users.
     */
    @Column(name = "system_builtin", nullable = false)
    private Boolean systemBuiltin;

    /**
     * Optional remark or description.
     */
    @Column(name = "remark", length = 512)
    private String remark;

    /**
     * Creation timestamp.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Last update timestamp.
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * JPA 构造函数。
     */
    protected Resource() {
    }

    /**
     * Creates a new resource.
     *
     * @param code          unique resource code
     * @param parentId      parent resource ID, null for root resources
     * @param name          resource display name
     * @param resourceType  resource type (DIRECTORY, MENU, FUNCTION, or BUTTON)
     * @param path          front-end route path
     * @param component     front-end component path
     * @param icon          icon identifier
     * @param sortNo        sort order among siblings
     * @param visible       whether the resource is visible
     * @param defaultEntry  whether this is the default entry
     * @param systemBuiltin whether this resource is system-built
     * @param remark        optional remark
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
     * Updates the resource's basic information (name, icon, remark).
     *
     * @param name   the new display name
     * @param icon   the new icon identifier
     * @param remark the new remark
     */
    public void modifyBasicInfo(String name, String icon, String remark) {
        this.name = requireNotBlank(name, "资源名称不能为空");
        this.icon = normalizeBlank(icon);
        this.remark = normalizeBlank(remark);
        touch();
    }

    /**
     * Changes the resource code.
     *
     * @param code the new unique resource code
     */
    public void changeCode(String code) {
        this.code = requireNotBlank(code, "资源编码不能为空");
        touch();
    }

    /**
     * Changes the resource type.
     *
     * @param resourceType the new resource type
     */
    public void changeType(ResourceType resourceType) {
        this.resourceType = requireNonNull(resourceType, "资源类型不能为空");
        touch();
    }

    /**
     * Updates the front-end route information (path and component).
     *
     * <p>Typically applicable to MENU or FUNCTION resources.</p>
     *
     * @param path      the new front-end route path
     * @param component the new front-end component path
     */
    public void modifyRoute(String path, String component) {
        this.path = normalizeBlank(path);
        this.component = normalizeBlank(component);
        touch();
    }

    /**
     * Moves this resource under a new parent.
     *
     * <p>Type compatibility should be validated by the domain service
     * using {@link ResourceType#canHaveChild(ResourceType)}.</p>
     *
     * @param parentId the new parent resource ID
     */
    public void moveTo(Long parentId) {
        this.parentId = parentId;
        touch();
    }

    /**
     * Changes the sort order.
     *
     * @param sortNo the new sort order value
     */
    public void changeSortNo(Integer sortNo) {
        this.sortNo = requireNonNull(sortNo, "排序号不能为空");
        touch();
    }

    /**
     * Enables this resource.
     */
    public void enable() {
        this.enabled = true;
        touch();
    }

    /**
     * Disables this resource.
     */
    public void disable() {
        this.enabled = false;
        touch();
    }

    /**
     * Makes this resource visible.
     */
    public void show() {
        this.visible = true;
        touch();
    }

    /**
     * Hides this resource.
     */
    public void hide() {
        this.visible = false;
        touch();
    }

    /**
     * Marks this resource as the default entry point.
     */
    public void markDefaultEntry() {
        this.defaultEntry = true;
        touch();
    }

    /**
     * Removes the default entry status from this resource.
     */
    public void cancelDefaultEntry() {
        this.defaultEntry = false;
        touch();
    }

    public void markSystemBuiltin() {
        this.systemBuiltin = true;
        touch();
    }

    public void cancelSystemBuiltin() {
        this.systemBuiltin = false;
        touch();
    }

    /**
     * Checks whether this resource is a directory.
     *
     * @return true if this is a DIRECTORY-type resource
     */
    public boolean isDirectory() {
        return this.resourceType == ResourceType.DIRECTORY;
    }

    /**
     * Checks whether this resource is a menu.
     *
     * @return true if this is a MENU-type resource
     */
    public boolean isMenu() {
        return this.resourceType == ResourceType.MENU;
    }

    /**
     * Checks whether this resource is a function.
     *
     * @return true if this is a FUNCTION-type resource
     */
    public boolean isFunction() {
        return this.resourceType == ResourceType.FUNCTION;
    }

    /**
     * Checks whether this resource is a button.
     *
     * @return true if this is a BUTTON-type resource
     */
    public boolean isButton() {
        return this.resourceType == ResourceType.BUTTON;
    }

    /**
     * Checks whether this resource can have child resources.
     *
     * @return true if this resource can have children
     */
    public boolean canHaveChildren() {
        return this.resourceType.canHaveChildren();
    }

    /**
     * Checks whether this resource can have a child of the specified type.
     *
     * @param childType the child resource type to check
     * @return true if the specified child type is allowed
     */
    public boolean canHaveChild(ResourceType childType) {
        return this.resourceType.canHaveChild(childType);
    }

    /**
     * Checks whether this resource can bind permission codes.
     *
     * @return true if this resource can bind permissions
     */
    public boolean canBindPermission() {
        return this.resourceType.canBindPermission();
    }

    /**
     * Updates the timestamp to mark the resource as modified.
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

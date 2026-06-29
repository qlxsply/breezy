package com.corwin.system.resource.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * 资源权限码关系。
 *
 * <p>
 * 用于描述资源按钮与权限码之间的绑定关系。
 * </p>
 *
 * <p>
 * 当前资源模型中，资源树由 {@link Resource} 表示，权限码由 {@link Permission} 表示。
 * 本实体负责建立二者之间的关联关系。
 * </p>
 *
 * <p>
 * 关系说明：
 * </p>
 *
 * <ul>
 *     <li>{@code resourceId}：关联 {@link Resource#getId()}。</li>
 *     <li>{@code permissionId}：关联 {@link Permission#getId()}。</li>
 *     <li>业务上要求 {@code resourceId} 对应的资源类型必须是 {@link ResourceType#BUTTON}。</li>
 *     <li>一个按钮资源可以绑定一个或多个权限码。</li>
 *     <li>一个权限码可以被多个按钮资源复用。</li>
 * </ul>
 *
 * <p>
 * 注意：
 * </p>
 *
 * <ul>
 *     <li>permission 不是资源树节点。</li>
 *     <li>permission 不应该作为 {@link ResourceType} 的一种类型。</li>
 *     <li>只有 BUTTON 类型资源允许绑定权限码。</li>
 *     <li>本实体只保存绑定关系，不负责校验资源类型。</li>
 *     <li>资源类型校验应由领域服务完成。</li>
 * </ul>
 *
 *
 * <p>
 * 示例：
 * </p>
 *
 * <pre>
 * Resource:
 *   id = 1001
 *   code = system.user.create
 *   resourceType = BUTTON
 *
 * Permission:
 *   id = 2001
 *   code = system:user:create
 *
 * ResourcePermission:
 *   resourceId = 1001
 *   permissionId = 2001
 * </pre>
 *
 * @author Corwin
 */
@Getter
@Entity
@Table(name = "sys_resource_permission", indexes = {
        @Index(name = "idx_sys_resource_permission_unique", columnList = "resource_id,permission_id", unique = true),
        @Index(name = "idx_sys_resource_permission_resource_id", columnList = "resource_id"),
        @Index(name = "idx_sys_resource_permission_permission_id", columnList = "permission_id")})
public class ResourcePermission {

    /**
     * 主键 ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 资源 ID。
     *
     * <p>
     * 关联 {@link Resource#getId()}。
     * </p>
     *
     * <p>
     * 业务约束：
     * </p>
     *
     * <ul>
     *     <li>该资源必须存在。</li>
     *     <li>该资源必须启用。</li>
     *     <li>该资源类型必须是 {@link ResourceType#BUTTON}。</li>
     * </ul>
     *
     * <p>
     * 不在本实体中直接使用 {@code @ManyToOne}，是为了保持领域模型简单，
     * 避免资源树加载时产生不必要的级联查询。
     * </p>
     */
    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    /**
     * 权限码 ID。
     *
     * <p>
     * 关联 {@link Permission#getId()}。
     * </p>
     *
     * <p>
     * 权限码的具体业务编码、名称、用户范围等信息由 {@link Permission} 维护。
     * 本实体只保存按钮资源与权限码之间的绑定关系。
     * </p>
     *
     * <p>
     * 业务约束：
     * </p>
     *
     * <ul>
     *     <li>该权限码必须存在。</li>
     *     <li>该权限码必须启用。</li>
     * </ul>
     */
    @Column(name = "permission_id", nullable = false)
    private Long permissionId;

    /**
     * 是否系统内置绑定关系。
     *
     * <p>
     * 系统内置绑定关系通常由初始化脚本、XML 资源定义或系统启动导入流程生成。
     * 这类绑定关系一般不允许普通用户删除。
     * </p>
     */
    @Column(name = "system_builtin", nullable = false)
    private Boolean systemBuiltin;

    /**
     * 创建时间。
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * JPA 构造函数。
     */
    protected ResourcePermission() {
    }

    /**
     * 创建资源权限码绑定关系。
     *
     * @param resourceId    资源 ID，业务上必须对应 BUTTON 类型资源
     * @param permissionId  权限码 ID，对应 {@link Permission#getId()}
     * @param systemBuiltin 是否系统内置绑定关系
     */
    public ResourcePermission(Long resourceId, Long permissionId, Boolean systemBuiltin) {
        this.resourceId = requireNonNull(resourceId, "资源 ID 不能为空");
        this.permissionId = requireNonNull(permissionId, "权限码 ID 不能为空");
        this.systemBuiltin = defaultBoolean(systemBuiltin, false);
        this.createdAt = HighDate.mockInstant();
    }

    /**
     * 判断当前绑定关系是否属于指定资源。
     *
     * @param resourceId 资源 ID
     * @return true 表示当前绑定关系属于指定资源
     */
    public boolean belongsToResource(Long resourceId) {
        return this.resourceId != null && this.resourceId.equals(resourceId);
    }

    /**
     * 判断当前绑定关系是否指向指定权限码。
     *
     * @param permissionId 权限码 ID
     * @return true 表示当前绑定关系指向指定权限码
     */
    public boolean pointsToPermission(Long permissionId) {
        return this.permissionId != null && this.permissionId.equals(permissionId);
    }

    /**
     * 判断当前绑定关系是否为系统内置。
     *
     * @return true 表示系统内置绑定关系
     */
    public boolean isSystemBuiltin() {
        return Boolean.TRUE.equals(this.systemBuiltin);
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
}

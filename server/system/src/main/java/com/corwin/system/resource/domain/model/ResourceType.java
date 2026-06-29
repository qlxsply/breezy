package com.corwin.system.resource.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 * 资源类型。
 *
 * <p>
 * 用于统一描述管理后台资源树中的节点类型。
 * </p>
 *
 * <p>
 * 资源树结构约束：
 * </p>
 *
 * <pre>
 * DIRECTORY 目录
 * ├── DIRECTORY 子目录
 * └── MENU      菜单
 *
 * MENU 菜单
 * ├── MENU      子菜单
 * ├── FUNCTION  功能
 * └── BUTTON    按钮
 *
 * FUNCTION 功能
 * └── BUTTON    按钮
 *
 * BUTTON 按钮
 * └── 不允许有任何资源子节点
 * </pre>
 *
 * <p>
 * 说明：
 * </p>
 *
 * <ul>
 *     <li>DIRECTORY：目录资源，主要用于导航分组或资源分组。</li>
 *     <li>MENU：菜单资源，通常对应一个前端路由页面或菜单入口。</li>
 *     <li>FUNCTION：功能资源，通常表示菜单下的子页面能力、隐藏页面能力或功能分组。</li>
 *     <li>BUTTON：按钮资源，权限控制的最小 UI 操作节点。</li>
 * </ul>
 *
 * <p>
 * 注意：
 * </p>
 *
 * <ul>
 *     <li>权限码 permission 不是资源树节点。</li>
 *     <li>只有 BUTTON 类型资源可以绑定权限码。</li>
 *     <li>权限码通过 {@link ResourcePermission} 进行绑定。</li>
 * </ul>
 *
 * @author Corwin
 */
public enum ResourceType implements DictEnumDefinition {

    /**
     * 目录。
     *
     * <p>
     * 目录通常不对应具体页面，只用于菜单分组。
     * 目录下面只能挂载目录或菜单。
     * </p>
     */
    DIRECTORY("目录", DictTagColor.SLATE, DictTagType.INFO),

    /**
     * 菜单。
     *
     * <p>
     * 菜单通常对应一个前端页面、路由入口或可见导航项。
     * 菜单下面可以挂载子菜单、功能或按钮。
     * </p>
     */
    MENU("菜单", DictTagColor.PRIMARY_BLUE, DictTagType.INFO),

    /**
     * 功能。
     *
     * <p>
     * 功能表示菜单下的子页面能力、隐藏能力、功能分组或业务能力。
     * 功能下面只能挂载按钮。
     * </p>
     */
    FUNCTION("功能", DictTagColor.SUCCESS_GREEN, DictTagType.SUCCESS),

    /**
     * 按钮。
     *
     * <p>
     * 按钮是资源树的最低层级节点。
     * 按钮下面不允许再挂载资源节点。
     * 只有按钮可以绑定一个或多个 API 权限码。
     * </p>
     */
    BUTTON("按钮", DictTagColor.WARNING_ORANGE, DictTagType.WARNING);

    private final String label;

    private final DictTagColor tagColor;

    private final DictTagType tagType;

    ResourceType(String label, DictTagColor tagColor, DictTagType tagType) {
        this.label = label;
        this.tagColor = tagColor;
        this.tagType = tagType;
    }

    /**
     * 判断当前类型是否允许挂载指定类型的子资源。
     *
     * @param childType 子资源类型
     * @return true 表示允许挂载，false 表示不允许挂载
     */
    public boolean canHaveChild(ResourceType childType) {
        if (childType == null) {
            return false;
        }

        return switch (this) {
            case DIRECTORY -> childType == DIRECTORY || childType == MENU;
            case MENU -> childType == MENU || childType == FUNCTION || childType == BUTTON;
            case FUNCTION -> childType == BUTTON;
            case BUTTON -> false;
        };
    }

    /**
     * 判断当前类型是否可以拥有子资源。
     *
     * @return true 表示可以拥有子资源，false 表示不能拥有子资源
     */
    public boolean canHaveChildren() {
        return this != BUTTON;
    }

    /**
     * 判断当前类型是否允许绑定权限码。
     *
     * <p>
     * 当前模型中，只有按钮资源允许绑定权限码。
     * </p>
     *
     * @return true 表示允许绑定权限码，false 表示不允许绑定权限码
     */
    public boolean canBindPermission() {
        return this == BUTTON;
    }

    /**
     * 判断当前类型是否是导航类资源。
     *
     * <p>
     * DIRECTORY 和 MENU 通常参与左侧菜单树、顶部菜单树或导航树渲染。
     * FUNCTION 和 BUTTON 通常不直接作为主导航项展示。
     * </p>
     *
     * @return true 表示是导航类资源，false 表示不是导航类资源
     */
    public boolean isNavigationResource() {
        return this == DIRECTORY || this == MENU;
    }

    /**
     * 判断当前类型是否是页面或页面能力类资源。
     *
     * <p>
     * MENU 通常表示页面入口。
     * FUNCTION 通常表示菜单下的子页面、隐藏页面或功能分组。
     * </p>
     *
     * @return true 表示是页面或页面能力类资源
     */
    public boolean isPageLikeResource() {
        return this == MENU || this == FUNCTION;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public String tagColor() {
        return tagColor.itemValue();
    }

    @Override
    public String tagType() {
        return tagType.itemValue();
    }
}

package com.corwin.system.resource.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * 菜单
 *
 * @author Corwin 2026/4/19
 */
@Getter
@Entity
@Table(name = "sys_menu", indexes = {@Index(name = "idx_sys_menu_code", columnList = "code", unique = true),
        @Index(name = "idx_sys_menu_parent_id", columnList = "parent_id"),
        @Index(name = "idx_sys_menu_sort_no", columnList = "sort_no")})
public class Menu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 128)
    private String code;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "path", length = 512)
    private String path;

    @Column(name = "component", length = 512)
    private String component;

    @Column(name = "icon", length = 128)
    private String icon;

    @Enumerated(EnumType.STRING)
    @Column(name = "menu_type", length = 32)
    private MenuType menuType;

    @Column(name = "sort_no", nullable = false)
    private Integer sortNo;

    @Column(name = "visible", nullable = false)
    private Boolean visible;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "system_builtin", nullable = false)
    private Boolean systemBuiltin;

    @Column(name = "remark", length = 512)
    private String remark;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Menu() {
    }

    public Menu(String code, Long parentId, String name, String path, String component, String icon, MenuType menuType,
            Integer sortNo, Boolean visible, Boolean systemBuiltin, String remark) {
        Instant now = HighDate.mockInstant();
        this.code = code;
        this.parentId = parentId;
        this.name = name;
        this.path = path;
        this.component = component;
        this.icon = icon;
        this.menuType = menuType;
        this.sortNo = sortNo;
        this.visible = visible;
        this.enabled = true;
        this.systemBuiltin = systemBuiltin;
        this.remark = remark;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void enable() {
        this.enabled = true;
        touch();
    }

    public void disable() {
        this.enabled = false;
        touch();
    }

    public void show() {
        this.visible = true;
        touch();
    }

    public void hide() {
        this.visible = false;
        touch();
    }

    public MenuType resolveMenuType() {
        if (menuType != null) {
            return menuType;
        }
        return (path == null || path.isBlank()) && (component == null || component.isBlank())
                ? MenuType.DIRECTORY
                : MenuType.MENU;
    }

    private void touch() {
        this.updatedAt = HighDate.mockInstant();
    }
}

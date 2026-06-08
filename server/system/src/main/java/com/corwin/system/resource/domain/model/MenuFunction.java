package com.corwin.system.resource.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 *
 * @author Corwin 2026/4/19
 */
@Getter
@Entity
@Table(name = "sys_menu_function",
        indexes = {@Index(name = "idx_sys_menu_function_unique", columnList = "menu_id,function_id", unique = true),
                @Index(name = "idx_sys_menu_function_menu_id", columnList = "menu_id"),
                @Index(name = "idx_sys_menu_function_function_id", columnList = "function_id"),
                @Index(name = "idx_sys_menu_function_parent_id", columnList = "parent_id")})
public class MenuFunction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "menu_id", nullable = false)
    private Long menuId;

    @Column(name = "function_id", nullable = false)
    private Long functionId;

    /**
     * 父挂载节点 ID，不是父功能 ID。
     */
    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "sort_no", nullable = false)
    private Integer sortNo;

    @Column(name = "visible", nullable = false)
    private Boolean visible;

    @Column(name = "default_entry", nullable = false)
    private Boolean defaultEntry;

    @Column(name = "system_builtin", nullable = false)
    private Boolean systemBuiltin;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MenuFunction() {
    }

    public MenuFunction(Long menuId, Long functionId, Long parentId, Integer sortNo, Boolean visible,
            Boolean defaultEntry, Boolean systemBuiltin) {
        Instant now = HighDate.mockInstant();
        this.menuId = menuId;
        this.functionId = functionId;
        this.parentId = parentId;
        this.sortNo = sortNo;
        this.visible = visible;
        this.defaultEntry = defaultEntry;
        this.systemBuiltin = systemBuiltin;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void show() {
        this.visible = true;
        touch();
    }

    public void hide() {
        this.visible = false;
        touch();
    }

    public void markDefaultEntry() {
        this.defaultEntry = true;
        touch();
    }

    public void cancelDefaultEntry() {
        this.defaultEntry = false;
        touch();
    }

    private void touch() {
        this.updatedAt = HighDate.mockInstant();
    }
}

package com.corwin.system.role.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * 角色菜单关系
 *
 * @author Corwin 2026/4/19
 */
@Getter
@Entity
@Table(name = "sys_role_menu",
        indexes = {@Index(name = "idx_sys_role_menu_unique", columnList = "role_id,menu_id", unique = true),
                @Index(name = "idx_sys_role_menu_role_id", columnList = "role_id"),
                @Index(name = "idx_sys_role_menu_menu_id", columnList = "menu_id")})
public class RoleMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "menu_id", nullable = false)
    private Long menuId;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RoleMenu() {
    }

    public RoleMenu(Long roleId, Long menuId, Long operator) {
        this.roleId = roleId;
        this.menuId = menuId;
        this.createdBy = operator;
        this.createdAt = HighDate.mockInstant();
    }
}

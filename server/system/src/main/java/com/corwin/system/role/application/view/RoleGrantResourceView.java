package com.corwin.system.role.application.view;

/**
 * @author Corwin 2026/5/7
 */
public record RoleGrantResourceView(
        String id,
        String parentId,
        String menuId,
        String functionId,
        String name,
        String code,
        String type,
        String description,
        boolean enabled,
        boolean selectable,
        int orderNo
) {
}

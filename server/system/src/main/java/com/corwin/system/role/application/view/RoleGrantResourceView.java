package com.corwin.system.role.application.view;

/**
 * View of a grantable resource with tree structure support.
 *
 * @author Corwin 2026/5/7
 */
public record RoleGrantResourceView(
        String id,
        String parentId,
        String resourceId,
        String name,
        String code,
        String type,
        String description,
        boolean enabled,
        boolean selectable,
        int orderNo
) {
}

package com.corwin.system.role.interfaces.web.res;

import java.util.List;

/**
 * @author Corwin 2026/5/7
 */
public record RoleGrantResourceRes(
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
        int orderNo,
        List<String> permissionCodes
) {
}

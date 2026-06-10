package com.corwin.system.resource.interfaces.web.res;

/**
 * @author Corwin 2026/5/31
 */
public record AdminMenuResourceRes(
        String id,
        String parentId,
        String name,
        String icon,
        String description,
        String code,
        String type,
        String scope,
        String openMode,
        String url,
        String loadTarget,
        int orderNo,
        String level,
        boolean enabled,
        boolean guestAccess
) {
}

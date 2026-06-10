package com.corwin.system.resource.interfaces.web.res;

/**
 * @author Corwin 2026/6/6
 */
public record UserToolPageRes(
        String id,
        String name,
        String icon,
        String description,
        String code,
        String path,
        String component,
        int sortNo,
        String level,
        boolean enabled,
        boolean guestAccess
) {
}

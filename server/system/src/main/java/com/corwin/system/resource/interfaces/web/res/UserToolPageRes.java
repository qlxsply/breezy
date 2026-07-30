package com.corwin.system.resource.interfaces.web.res;

/**
 * Response DTO representing a single tool page for the user portal.
 *
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

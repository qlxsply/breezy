package com.corwin.system.resource.application.view;

/**
 * @author Corwin 2026/6/6
 */
public record UserToolPageView(
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

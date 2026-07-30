package com.corwin.system.resource.application.view;

/**
 * View object representing a single tool page entry.
 *
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

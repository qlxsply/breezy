package com.corwin.system.resource.application.view;

/**
 * @author Corwin 2026/5/7
 */
public record RegistryResourceView(
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

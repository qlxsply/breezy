package com.corwin.system.userfeature.application.view;

import java.util.List;

/**
 * View object representing a product application with its features and permission binding count.
 *
 * @author Corwin 2026/6/14
 */
public record UserFeatureApplicationView(
        Long id,
        String code,
        String name,
        String description,
        String icon,
        String routePath,
        String componentPath,
        boolean enabled,
        int featureCount,
        int permissionBindingCount,
        List<UserFeatureItemView> features
) {
}

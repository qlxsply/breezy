package com.corwin.system.userfeature.interfaces.web.res;

import java.util.List;

/**
 * @author Corwin 2026/6/14
 */
public record UserFeatureApplicationRes(
        String id,
        String code,
        String name,
        String description,
        String icon,
        String routePath,
        String componentPath,
        boolean enabled,
        int featureCount,
        int permissionBindingCount,
        List<UserFeatureItemRes> features
) {
}

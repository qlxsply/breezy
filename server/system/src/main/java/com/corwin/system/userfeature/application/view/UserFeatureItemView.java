package com.corwin.system.userfeature.application.view;

import java.util.List;

/**
 * @author Corwin 2026/6/14
 */
public record UserFeatureItemView(
        Long id,
        Long applicationId,
        String code,
        String name,
        String description,
        boolean enabled,
        List<String> permissionCodes
) {
}
